<template>
  <div class="category-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资产分类管理</span>
          <el-button type="primary" @click="openAddDialog" :icon="Plus">新增分类</el-button>
        </div>
      </template>
      
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="categoryCode" label="分类编码" width="150" />
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column prop="depreciationPeriod" label="折旧年限(月)" width="120">
          <template #default="{ row }">
            {{ row.depreciationPeriod || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="residualRate" label="残值率(%)" width="100">
          <template #default="{ row }">
            {{ row.residualRate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="折旧年限">
          <el-input-number v-model="form.depreciationPeriod" :min="1" :max="360" />
          <span class="unit">月</span>
        </el-form-item>
        <el-form-item label="残值率">
          <el-input-number v-model="form.residualRate" :min="0" :max="100" :precision="2" />
          <span class="unit">%</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../utils/api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  categoryCode: '',
  categoryName: '',
  depreciationPeriod: 60,
  residualRate: 5.00,
  sort: 0,
  status: 1
})

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    tableData.value = res.data || []
  } catch {
    tableData.value = [
      { id: 1, categoryCode: 'PC', categoryName: '电脑', depreciationPeriod: 36, residualRate: 5.00, status: 1 },
      { id: 2, categoryCode: 'PRINT', categoryName: '打印机', depreciationPeriod: 36, residualRate: 5.00, status: 1 },
      { id: 3, categoryCode: 'DESK', categoryName: '办公桌', depreciationPeriod: 60, residualRate: 5.00, status: 1 },
      { id: 4, categoryCode: 'CHAIR', categoryName: '办公椅', depreciationPeriod: 60, residualRate: 5.00, status: 1 }
    ]
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    categoryCode: '',
    categoryName: '',
    depreciationPeriod: 60,
    residualRate: 5.00,
    sort: 0,
    status: 1
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await updateCategory(form.id, form)
          ElMessage.success('更新成功')
        } else {
          await addCategory(form)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadData()
      } catch {
        ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
      }
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除分类"${row.categoryName}"吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch {
      ElMessage.error('删除失败')
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

.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
