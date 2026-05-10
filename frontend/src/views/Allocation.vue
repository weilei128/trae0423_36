<template>
  <div class="allocation-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已归还" value="returned" />
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
        <el-table-column label="申请类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.applyType === 'receive' ? 'primary' : 'warning'">
              {{ row.applyType === 'receive' ? '领用' : '借用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyUserName" label="申请人" width="100" />
        <el-table-column prop="assignUserName" label="使用人" width="100" />
        <el-table-column prop="applyReason" label="申请原因" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button link type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
              <el-button link type="danger" size="small" @click="handleApprove(row, false)">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 'approved' && row.applyType === 'borrow'">
              <el-button link type="warning" size="small" @click="handleReturn(row)">归还</el-button>
            </template>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllocationList, approveAllocation, returnAllocation } from '../utils/api'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllocationList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = [
      { id: 1, assetCode: 'PC2024050001', assetName: '联想ThinkPad X1', applyType: 'receive', applyUserName: 'IT员工', assignUserName: '张三', applyReason: '办公需要', status: 'pending', createTime: '2024-05-01 10:00:00' },
      { id: 2, assetCode: 'PC2024050002', assetName: 'HP LaserJet Pro', applyType: 'borrow', applyUserName: '李四', assignUserName: '李四', applyReason: '会议使用', status: 'approved', createTime: '2024-05-02 14:30:00' }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝', returned: '已归还' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger', returned: 'info' }
  return map[status] || ''
}

const handleApprove = (row, approved) => {
  ElMessageBox.confirm(`确定要${approved ? '通过' : '拒绝'}该申请吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await approveAllocation(row.id, { approverId: 1, approved, remark: '' })
      ElMessage.success(approved ? '已通过' : '已拒绝')
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

const handleReturn = (row) => {
  ElMessageBox.confirm('确定要办理归还吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await returnAllocation(row.id)
      ElMessage.success('归还成功')
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

const viewDetail = (row) => {
  ElMessage.info('详情: ' + JSON.stringify(row))
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
