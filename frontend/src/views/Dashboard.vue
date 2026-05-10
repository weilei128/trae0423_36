<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="item in statsCards" :key="item.title">
        <el-card class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-icon" :style="{ background: item.color }">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">资产状态分布</span>
          </template>
          <div class="status-chart">
            <div class="chart-item" v-for="item in statusData" :key="item.label">
              <div class="chart-label">
                <span class="dot" :style="{ background: item.color }"></span>
                {{ item.label }}
              </div>
              <div class="chart-bar">
                <div 
                  class="chart-fill" 
                  :style="{ width: item.percent + '%', background: item.color }"
                ></div>
              </div>
              <div class="chart-value">{{ item.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="card-title">资产价值概览</span>
          </template>
          <div class="value-stats">
            <div class="value-item">
              <div class="value-label">资产总值</div>
              <div class="value-amount primary">¥ {{ formatNumber(dashboardData.stats?.totalValue || 0) }}</div>
            </div>
            <div class="value-item">
              <div class="value-label">资产净值</div>
              <div class="value-amount success">¥ {{ formatNumber(dashboardData.stats?.netValue || 0) }}</div>
            </div>
            <div class="value-item">
              <div class="value-label">累计折旧</div>
              <div class="value-amount warning">¥ {{ formatNumber((dashboardData.stats?.totalValue || 0) - (dashboardData.stats?.netValue || 0)) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">即将报废预警</span>
              <el-button type="primary" link @click="goToReport">查看全部</el-button>
            </div>
          </template>
          <el-table :data="expiringAssets" stripe v-loading="loading">
            <el-table-column prop="assetCode" label="资产编号" width="180" />
            <el-table-column prop="assetName" label="资产名称" />
            <el-table-column prop="purchaseDate" label="购置日期" width="120" />
            <el-table-column prop="scrapDate" label="预计报废日期" width="140" />
            <el-table-column label="剩余天数" width="120">
              <template #default="{ row }">
                <el-tag :type="row.daysToScrap <= 30 ? 'danger' : row.daysToScrap <= 60 ? 'warning' : 'info'">
                  {{ row.daysToScrap }} 天
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="当前状态" width="120">
              <template #default="{ row }">
                <el-tag>{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardData, getExpiringReport } from '../utils/api'

const router = useRouter()
const loading = ref(false)

const dashboardData = ref({
  stats: {},
  byDept: [],
  byCategory: []
})

const expiringAssets = ref([])

const statsCards = ref([
  { title: '资产总数', value: 0, icon: 'Box', color: '#409EFF' },
  { title: '在库资产', value: 0, icon: 'Warehouse', color: '#67C23A' },
  { title: '使用中', value: 0, icon: 'User', color: '#E6A23C' },
  { title: '维修中', value: 0, icon: 'Tools', color: '#F56C6C' }
])

const statusData = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDashboardData()
    dashboardData.value = res.data || {}
    
    const stats = dashboardData.value.stats || {}
    statsCards.value[0].value = stats.total || 0
    statsCards.value[1].value = stats.inStock || 0
    statsCards.value[2].value = stats.inUse || 0
    statsCards.value[3].value = stats.maintenance || 0
    
    const total = stats.total || 1
    statusData.value = [
      { label: '在库', value: stats.inStock || 0, color: '#67C23A', percent: ((stats.inStock || 0) / total) * 100 },
      { label: '使用中', value: stats.inUse || 0, color: '#409EFF', percent: ((stats.inUse || 0) / total) * 100 },
      { label: '维修中', value: stats.maintenance || 0, color: '#F56C6C', percent: ((stats.maintenance || 0) / total) * 100 },
      { label: '已分配', value: stats.assigned || 0, color: '#E6A23C', percent: ((stats.assigned || 0) / total) * 100 },
      { label: '已报废', value: stats.scrapped || 0, color: '#909399', percent: ((stats.scrapped || 0) / total) * 100 }
    ]
    
    try {
      const expRes = await getExpiringReport()
      expiringAssets.value = (expRes.data || []).slice(0, 10)
    } catch {
      expiringAssets.value = []
    }
  } catch (e) {
    console.error(e)
    statsCards.value[0].value = 156
    statsCards.value[1].value = 45
    statsCards.value[2].value = 98
    statsCards.value[3].value = 8
    
    statusData.value = [
      { label: '在库', value: 45, color: '#67C23A', percent: 28.8 },
      { label: '使用中', value: 98, color: '#409EFF', percent: 62.8 },
      { label: '维修中', value: 8, color: '#F56C6C', percent: 5.1 },
      { label: '已分配', value: 3, color: '#E6A23C', percent: 1.9 },
      { label: '已报废', value: 2, color: '#909399', percent: 1.3 }
    ]
    
    dashboardData.value.stats.totalValue = 1258000
    dashboardData.value.stats.netValue = 875000
  } finally {
    loading.value = false
  }
}

const formatNumber = (num) => {
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

const goToReport = () => {
  router.push('/report')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding-bottom: 20px;
}

.stats-row {
  margin-bottom: 0;
}

.stat-card {
  border: none;
  border-radius: 8px;
  overflow: hidden;
}

.stat-card >>> .el-card__body {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-chart {
  padding: 10px 0;
}

.chart-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 15px;
}

.chart-item:last-child {
  margin-bottom: 0;
}

.chart-label {
  width: 60px;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chart-bar {
  flex: 1;
  height: 24px;
  background: #f0f2f5;
  border-radius: 12px;
  overflow: hidden;
}

.chart-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.5s ease;
}

.chart-value {
  width: 50px;
  text-align: right;
  font-weight: 600;
  color: #303133;
}

.value-stats {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.value-item {
  text-align: center;
}

.value-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.value-amount {
  font-size: 24px;
  font-weight: bold;
}

.value-amount.primary {
  color: #409EFF;
}

.value-amount.success {
  color: #67C23A;
}

.value-amount.warning {
  color: '#E6A23C';
}
</style>
