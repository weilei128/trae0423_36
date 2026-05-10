<template>
  <div class="transfer-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资产变动管理</span>
        </div>
      </template>
      
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="assetCode" label="资产编号" width="150" />
        <el-table-column prop="assetName" label="资产名称" />
        <el-table-column label="变动类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTransferType(row.transferType)">
              {{ getTransferTypeName(row.transferType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fromUserName" label="原使用人" width="100" />
        <el-table-column prop="toUserName" label="新使用人" width="100" />
        <el-table-column prop="transferReason" label="变动原因" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button link type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
              <el-button link type="danger" size="small" @click="handleApprove(row, false)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTransferList, approveTransfer } from '../utils/api'

const loading = ref(false)
const tableData = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTransferList({ current: 1, size: 100 })
    tableData.value = res.data?.records || []
  } catch {
    tableData.value = [
      { id: 1, assetCode: 'PC2024050001', assetName: '联想ThinkPad', transferType: 'transfer', fromUserName: '张三', toUserName: '李四', transferReason: '岗位调动', status: 'pending' },
      { id: 2, assetCode: 'DESK2024050001', assetName: '实木办公桌', transferType: 'transfer', fromUserName: '王五', toUserName: '赵六', transferReason: '部门调整', status: 'approved' }
    ]
  } finally {
    loading.value = false
  }
}

const getTransferTypeName = (type) => {
  const map = { transfer: '调拨', borrow: '借用', return: '归还' }
  return map[type] || type
}

const getTransferType = (type) => {
  const map = { transfer: 'primary', borrow: 'warning', return: 'info' }
  return map[type] || ''
}

const getStatusText = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || ''
}

const handleApprove = (row, approved) => {
  ElMessageBox.confirm(`确定要${approved ? '通过' : '拒绝'}该调拨申请吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await approveTransfer(row.id, { approverId: 1, approved })
      ElMessage.success(approved ? '已通过' : '已拒绝')
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
