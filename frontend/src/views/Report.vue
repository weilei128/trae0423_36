<template>
  <div class="report-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="资产汇总" name="summary">
        <el-card>
          <div class="summary-stats">
            <div class="stat-item">
              <div class="stat-label">资产总数</div>
              <div class="stat-value primary">{{ summaryData.totalAssets }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">在库</div>
              <div class="stat-value success">{{ summaryData.statusCount?.in_stock || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">使用中</div>
              <div class="stat-value info">{{ summaryData.statusCount?.in_use || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">维修中</div>
              <div class="stat-value warning">{{ summaryData.statusCount?.maintenance || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">已报废</div>
              <div class="stat-value danger">{{ summaryData.statusCount?.scrapped || 0 }}</div>
            </div>
          </div>
          
          <div style="margin-top: 30px;">
            <h3 class="section-title">各部门资产分布</h3>
            <el-table :data="summaryData.deptAssets || []" stripe>
              <el-table-column prop="deptId" label="部门ID" width="100" />
              <el-table-column prop="deptName" label="部门名称" />
              <el-table-column prop="assetCount" label="资产数量">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="getDeptPercent(row.assetCount)" 
                    :format="() => row.assetCount + ' 件'"
                    :stroke-width="24"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="报废预警" name="expiring">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>即将报废资产列表 (90天内)</span>
              <el-tag type="warning">定期检查，及时处理</el-tag>
            </div>
          </template>
          
          <el-table :data="expiringData" stripe v-loading="loading">
            <el-table-column prop="assetCode" label="资产编号" width="150" />
            <el-table-column prop="assetName" label="资产名称" />
            <el-table-column prop="purchaseDate" label="购置日期" width="120" />
            <el-table-column prop="scrapDate" label="预计报废日期" width="140" />
            <el-table-column label="剩余天数" width="120">
              <template #default="{ row }">
                <el-tag :type="getWarningType(row.daysToScrap)" size="large">
                  {{ row.daysToScrap }} 天
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="当前状态" width="100">
              <template #default="{ row }">
                <el-tag>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前净值" width="120">
              <template #default="{ row }">
                ¥{{ formatPrice(row.currentValue) }}
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-if="expiringData.length === 0 && !loading" description="暂无即将报废的资产" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getSummaryReport, getExpiringReport } from '../utils/api'

const activeTab = ref('summary')
const loading = ref(false)

const summaryData = reactive({
  totalAssets: 0,
  statusCount: {},
  deptAssets: []
})

const expiringData = ref([])

const totalDeptAssets = computed(() => {
  return (summaryData.deptAssets || []).reduce((sum, item) => sum + (item.assetCount || 0), 0) || 1
})

const getDeptPercent = (count) => {
  return Math.round((count || 0) / totalDeptAssets.value * 100)
}

const formatPrice = (val) => {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getStatusText = (status) => {
  const map = {
    'in_stock': '在库',
    'assigned': '已分配',
    'in_use': '使用中',
    'maintenance': '维修中',
    'scrapped': '已报废'
  }
  return map[status] || status
}

const getWarningType = (days) => {
  if (days <= 30) return 'danger'
  if (days <= 60) return 'warning'
  return 'info'
}

const loadSummary = async () => {
  try {
    const res = await getSummaryReport()
    Object.assign(summaryData, res.data || {
      totalAssets: 156,
      statusCount: { in_stock: 45, in_use: 98, maintenance: 8, scrapped: 5 },
      deptAssets: [
        { deptId: 2, deptName: 'IT部门', assetCount: 68 },
        { deptId: 3, deptName: '行政部门', assetCount: 45 },
        { deptId: 4, deptName: '财务部', assetCount: 28 },
        { deptId: 5, deptName: '人力资源部', assetCount: 15 }
      ]
    })
  } catch {
    summaryData.totalAssets = 156
    summaryData.statusCount = { in_stock: 45, in_use: 98, maintenance: 8, scrapped: 5 }
    summaryData.deptAssets = [
      { deptId: 2, deptName: 'IT部门', assetCount: 68 },
      { deptId: 3, deptName: '行政部门', assetCount: 45 },
      { deptId: 4, deptName: '财务部', assetCount: 28 },
      { deptId: 5, deptName: '人力资源部', assetCount: 15 }
    ]
  }
}

const loadExpiring = async () => {
  loading.value = true
  try {
    const res = await getExpiringReport()
    expiringData.value = res.data || []
  } catch {
    expiringData.value = [
      { id: 1, assetCode: 'PC2021050001', assetName: '联想台式机', purchaseDate: '2021-05-15', scrapDate: '2024-05-15', daysToScrap: 6, status: 'in_use', currentValue: 500 },
      { id: 2, assetCode: 'PRINT2021030001', assetName: 'HP LaserJet', purchaseDate: '2021-03-20', scrapDate: '2024-06-20', daysToScrap: 42, status: 'in_use', currentValue: 800 },
      { id: 3, assetCode: 'DESK2019010001', assetName: '实木办公桌', purchaseDate: '2019-01-10', scrapDate: '2024-07-10', daysToScrap: 62, status: 'in_use', currentValue: 300 }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadExpiring()
})
</script>

<style scoped>
.report-page {
  padding-bottom: 20px;
}

.summary-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  padding: 25px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  border-radius: 12px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-value.primary { color: #409EFF; }
.stat-value.success { color: #67C23A; }
.stat-value.info { color: #909399; }
.stat-value.warning { color: #E6A23C; }
.stat-value.danger { color: #F56C6C; }

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #409EFF;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
