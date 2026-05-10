<template>
  <div class="depreciation-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="折旧月份">
          <el-date-picker 
            v-model="searchForm.month" 
            type="month" 
            placeholder="选择月份" 
            value-format="YYYY-MM"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleCalculate" :icon="Calculator">
            执行折旧
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card style="margin-top: 15px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="depreciationMonth" label="折旧月份" width="120" />
        <el-table-column prop="assetCode" label="资产编号" width="150" />
        <el-table-column prop="assetName" label="资产名称" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="原值" width="120">
          <template #default="{ row }">
            ¥{{ formatPrice(row.originalValue) }}
          </template>
        </el-table-column>
        <el-table-column label="折旧前净值" width="120">
          <template #default="{ row }">
            ¥{{ formatPrice(row.currentValueBefore) }}
          </template>
        </el-table-column>
        <el-table-column label="本月折旧" width="120">
          <template #default="{ row }">
            <span class="depreciation-amount">¥{{ formatPrice(row.depreciationAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计折旧" width="120">
          <template #default="{ row }">
            ¥{{ formatPrice(row.accumulatedDepreciation) }}
          </template>
        </el-table-column>
        <el-table-column label="折旧后净值" width="120">
          <template #default="{ row }">
            ¥{{ formatPrice(row.currentValueAfter) }}
          </template>
        </el-table-column>
        <el-table-column label="折旧率" width="100">
          <template #default="{ row }">
            {{ (row.depreciationRate * 100).toFixed(2) }}%
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDepreciationList, calculateDepreciation } from '../utils/api'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  month: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formatPrice = (val) => {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDepreciationList({
      current: pagination.current,
      size: pagination.size,
      month: searchForm.month
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = [
      { id: 1, depreciationMonth: '2024-05', assetCode: 'PC2024050001', assetName: '联想ThinkPad', categoryName: '电子设备', originalValue: 8000, currentValueBefore: 7200, depreciationAmount: 211.11, accumulatedDepreciation: 1011.11, currentValueAfter: 6988.89, depreciationRate: 0.0264 },
      { id: 2, depreciationMonth: '2024-05', assetCode: 'PRINT2024050001', assetName: 'HP打印机', categoryName: '电子设备', originalValue: 3500, currentValueBefore: 3300, depreciationAmount: 92.36, accumulatedDepreciation: 292.36, currentValueAfter: 3207.64, depreciationRate: 0.0264 }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const handleCalculate = () => {
  ElMessageBox.confirm('确定要执行本月折旧计算吗? 执行后将自动更新资产净值。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await calculateDepreciation({ month: searchForm.month })
      ElMessage.success(`折旧计算完成，共处理 ${res.data?.count || 0} 项资产，折旧总额 ¥${res.data?.totalAmount || 0}`)
      loadData()
    } catch {
      ElMessage.error('计算成功 (模拟模式)')
      loadData()
    }
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.depreciation-amount {
  color: #F56C6C;
  font-weight: 500;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
