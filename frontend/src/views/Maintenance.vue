<template>
  <div class="maintenance-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待处理" value="pending" />
            <el-option label="维修中" value="repairing" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card style="margin-top: 15px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="assetCode" label="资产编号" width="150" />
        <el-table-column prop="assetName" label="资产名称" />
        <el-table-column prop="reportUserName" label="报修人" width="100" />
        <el-table-column prop="faultDescription" label="故障描述" show-overflow-tooltip />
        <el-table-column prop="faultLocation" label="故障地点" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maintenancePerson" label="维修人员" width="100" />
        <el-table-column label="维修费用" width="100">
          <template #default="{ row }">
            ¥{{ row.maintenanceCost || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleStartMaintenance(row)">开始维修</el-button>
            <el-button v-if="row.status === 'repairing'" link type="success" size="small" @click="openCompleteDialog(row)">完成维修</el-button>
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
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
    
    <el-dialog v-model="completeDialogVisible" title="完成维修" width="500px">
      <el-form ref="completeFormRef" :model="completeForm" label-width="100px">
        <el-form-item label="维修人员">
          <el-input v-model="completeForm.maintenancePerson" placeholder="请输入维修人员" />
        </el-form-item>
        <el-form-item label="维修内容">
          <el-input v-model="completeForm.maintenanceContent" type="textarea" :rows="3" placeholder="请输入维修内容" />
        </el-form-item>
        <el-form-item label="配件费用">
          <el-input-number v-model="completeForm.partsCost" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="人工费用">
          <el-input-number v-model="completeForm.laborCost" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaintenanceList, startMaintenance as apiStartMaintenance, completeMaintenance as apiCompleteMaintenance } from '../utils/api'

const loading = ref(false)
const tableData = ref([])
const completeDialogVisible = ref(false)
const completeFormRef = ref()
const currentMaintenanceId = ref(null)

const searchForm = reactive({ status: '' })

const pagination = reactive({ current: 1, size: 10, total: 0 })

const completeForm = reactive({
  maintenancePerson: '',
  maintenanceContent: '',
  partsCost: 0,
  laborCost: 0,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMaintenanceList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = [
      { id: 1, assetCode: 'PC2024050001', assetName: '联想ThinkPad', reportUserName: '张三', faultDescription: '无法开机', faultLocation: '3楼办公区', status: 'pending', maintenanceCost: 0 },
      { id: 2, assetCode: 'PRINT2024050001', assetName: 'HP打印机', reportUserName: '李四', faultDescription: '卡纸频繁', faultLocation: '2楼会议室', status: 'repairing', maintenanceCost: 0 },
      { id: 3, assetCode: 'DESK2024050001', assetName: '办公椅', reportUserName: '王五', faultDescription: '轮子损坏', faultLocation: 'IT部', status: 'completed', maintenanceCost: 150, maintenancePerson: '维修师傅A' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const map = { pending: '待处理', repairing: '维修中', completed: '已完成' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', repairing: 'primary', completed: 'success' }
  return map[status] || ''
}

const handleStartMaintenance = async (row) => {
  try {
    await apiStartMaintenance(row.id)
    ElMessage.success('已开始维修')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const openCompleteDialog = (row) => {
  currentMaintenanceId.value = row.id
  Object.assign(completeForm, {
    maintenancePerson: '',
    maintenanceContent: '',
    partsCost: 0,
    laborCost: 0,
    remark: ''
  })
  completeDialogVisible.value = true
}

const submitComplete = async () => {
  completeForm.maintenanceCost = (completeForm.partsCost || 0) + (completeForm.laborCost || 0)
  try {
    await apiCompleteMaintenance(currentMaintenanceId.value, completeForm)
    ElMessage.success('维修完成')
    completeDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const viewDetail = (row) => {
  ElMessage.info('故障描述: ' + row.faultDescription)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
