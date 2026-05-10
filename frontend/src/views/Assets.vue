<template>
  <div class="assets-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="资产编号/名称/品牌" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="在库" value="in_stock" />
            <el-option label="已分配" value="assigned" />
            <el-option label="使用中" value="in_use" />
            <el-option label="维修中" value="maintenance" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData" :icon="Search">查询</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
          <el-button type="success" @click="openAddDialog" :icon="Plus">新增资产</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card style="margin-top: 15px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="assetCode" label="资产编号" width="160" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="purchaseDate" label="购置日期" width="110" />
        <el-table-column label="购置价格" width="110">
          <template #default="{ row }">
            ¥{{ formatPrice(row.purchasePrice) }}
          </template>
        </el-table-column>
        <el-table-column label="当前净值" width="110">
          <template #default="{ row }">
            ¥{{ formatPrice(row.currentValue) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsibleUserName" label="责任人" width="90" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button 
              v-if="row.status === 'in_stock'" 
              link type="warning" 
              size="small" 
              @click="openAllocationDialog(row)"
            >
              分配
            </el-button>
            <el-button 
              v-if="row.status === 'in_use' || row.status === 'assigned'" 
              link type="success" 
              size="small" 
              @click="openMaintenanceDialog(row)"
            >
              报修
            </el-button>
            <el-button 
              v-if="row.status !== 'scrapped'" 
              link type="danger" 
              size="small" 
              @click="handleScrap(row)"
            >
              报废
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="assetDialogVisible" :title="isEdit ? '编辑资产' : '新增资产'" width="700px">
      <el-form ref="assetFormRef" :model="assetForm" :rules="assetRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="assetForm.assetName" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="assetForm.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="assetForm.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="assetForm.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号">
              <el-input v-model="assetForm.sn" placeholder="请输入序列号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="assetForm.spec" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购置日期" prop="purchaseDate">
              <el-date-picker 
                v-model="assetForm.purchaseDate" 
                type="date" 
                placeholder="请选择购置日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购置价格" prop="purchasePrice">
              <el-input-number 
                v-model="assetForm.purchasePrice" 
                :min="0" 
                :precision="2"
                style="width: 100%"
                placeholder="请输入价格"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-input v-model="assetForm.supplier" placeholder="请输入供应商" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放地点">
              <el-input v-model="assetForm.location" placeholder="请输入存放地点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input 
                v-model="assetForm.description" 
                type="textarea" 
                :rows="3"
                placeholder="请输入描述"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="assetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAsset">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 分配对话框 -->
    <el-dialog v-model="allocationDialogVisible" title="资产分配" width="500px">
      <el-form ref="allocationFormRef" :model="allocationForm" :rules="allocationRules" label-width="100px">
        <el-form-item label="资产信息">
          <el-input :value="selectedAsset?.assetName" disabled />
        </el-form-item>
        <el-form-item label="申请类型" prop="applyType">
          <el-radio-group v-model="allocationForm.applyType">
            <el-radio label="receive">领用</el-radio>
            <el-radio label="borrow">借用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用人" prop="assignUserId">
          <el-select v-model="allocationForm.assignUserId" placeholder="请选择使用人" style="width: 100%" filterable>
            <el-option v-for="u in userList" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用部门" prop="assignDeptId">
          <el-select v-model="allocationForm.assignDeptId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因" prop="applyReason">
          <el-input 
            v-model="allocationForm.applyReason" 
            type="textarea" 
            :rows="3"
            placeholder="请输入申请原因"
          />
        </el-form-item>
        <el-form-item v-if="allocationForm.applyType === 'borrow'" label="预计归还">
          <el-date-picker 
            v-model="allocationForm.expectedReturnDate" 
            type="date" 
            placeholder="请选择预计归还日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="allocationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAllocation">提交申请</el-button>
      </template>
    </el-dialog>
    
    <!-- 报修对话框 -->
    <el-dialog v-model="maintenanceDialogVisible" title="资产报修" width="500px">
      <el-form ref="maintenanceFormRef" :model="maintenanceForm" :rules="maintenanceRules" label-width="100px">
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input 
            v-model="maintenanceForm.faultDescription" 
            type="textarea" 
            :rows="4"
            placeholder="请详细描述故障情况"
          />
        </el-form-item>
        <el-form-item label="故障地点">
          <el-input v-model="maintenanceForm.faultLocation" placeholder="请输入故障地点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintenanceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMaintenance">提交报修</el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="资产详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="资产编号">{{ detailData.assetCode }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detailData.assetName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detailData.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detailData.brand }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detailData.model }}</el-descriptions-item>
        <el-descriptions-item label="序列号">{{ detailData.sn }}</el-descriptions-item>
        <el-descriptions-item label="购置日期">{{ detailData.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="购置价格">¥{{ formatPrice(detailData.purchasePrice) }}</el-descriptions-item>
        <el-descriptions-item label="当前净值">¥{{ formatPrice(detailData.currentValue) }}</el-descriptions-item>
        <el-descriptions-item label="累计折旧">¥{{ formatPrice(detailData.accumulatedDepreciation) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="责任人">{{ detailData.responsibleUserName }}</el-descriptions-item>
        <el-descriptions-item label="责任部门">{{ detailData.responsibleDeptName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailData.supplier }}</el-descriptions-item>
        <el-descriptions-item label="存放地点">{{ detailData.location }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detailData.description }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAssetList, addAsset, updateAsset, scrapAsset,
  getCategoryList, getUsers, getDepts,
  createAllocation, createMaintenance
} from '../utils/api'

const loading = ref(false)
const tableData = ref([])
const categoryList = ref([])
const userList = ref([])
const deptList = ref([])

const searchForm = reactive({
  keyword: '',
  categoryId: null,
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const assetDialogVisible = ref(false)
const isEdit = ref(false)
const assetFormRef = ref()
const assetForm = reactive({
  id: null,
  assetName: '',
  categoryId: null,
  brand: '',
  model: '',
  sn: '',
  spec: '',
  purchaseDate: '',
  purchasePrice: null,
  supplier: '',
  location: '',
  description: ''
})

const assetRules = {
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购置日期', trigger: 'change' }],
  purchasePrice: [{ required: true, message: '请输入购置价格', trigger: 'blur' }]
}

const allocationDialogVisible = ref(false)
const selectedAsset = ref(null)
const allocationFormRef = ref()
const allocationForm = reactive({
  assetId: null,
  applyUserId: 1,
  applyType: 'receive',
  assignUserId: null,
  assignDeptId: null,
  applyReason: '',
  expectedReturnDate: ''
})

const allocationRules = {
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  applyReason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }]
}

const maintenanceDialogVisible = ref(false)
const maintenanceFormRef = ref()
const maintenanceForm = reactive({
  assetId: null,
  reportUserId: 1,
  faultDescription: '',
  faultLocation: ''
})

const maintenanceRules = {
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
}

const detailDialogVisible = ref(false)
const detailData = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }
    const res = await getAssetList(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error(e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadCategoryList = async () => {
  try {
    const res = await getCategoryList()
    categoryList.value = res.data || []
  } catch {
    categoryList.value = [
      { id: 1, categoryName: '电子设备' },
      { id: 2, categoryName: '办公家具' },
      { id: 3, categoryName: '车辆' }
    ]
  }
}

const loadUserList = async () => {
  try {
    const res = await getUsers({})
    userList.value = res.data || []
  } catch {
    userList.value = [
      { id: 1, realName: '系统管理员' },
      { id: 2, realName: '资产管理员' },
      { id: 3, realName: 'IT员工' }
    ]
  }
}

const loadDeptList = async () => {
  try {
    const res = await getDepts()
    deptList.value = res.data || []
  } catch {
    deptList.value = [
      { id: 1, deptName: '总公司' },
      { id: 2, deptName: 'IT部门' },
      { id: 3, deptName: '行政部门' }
    ]
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.status = ''
  loadData()
}

const openAddDialog = () => {
  isEdit.value = false
  Object.assign(assetForm, {
    id: null,
    assetName: '',
    categoryId: null,
    brand: '',
    model: '',
    sn: '',
    spec: '',
    purchaseDate: '',
    purchasePrice: null,
    supplier: '',
    location: '',
    description: ''
  })
  assetDialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(assetForm, {
    id: row.id,
    assetName: row.assetName,
    categoryId: row.categoryId,
    brand: row.brand,
    model: row.model,
    sn: row.sn,
    spec: row.spec,
    purchaseDate: row.purchaseDate,
    purchasePrice: row.purchasePrice,
    supplier: row.supplier,
    location: row.location,
    description: row.description
  })
  assetDialogVisible.value = true
}

const saveAsset = async () => {
  if (!assetFormRef.value) return
  await assetFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await updateAsset(assetForm.id, assetForm)
          ElMessage.success('更新成功')
        } else {
          await addAsset(assetForm)
          ElMessage.success('新增成功，资产已入库')
        }
        assetDialogVisible.value = false
        loadData()
      } catch (e) {
        ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
      }
    }
  })
}

const openAllocationDialog = (row) => {
  selectedAsset.value = row
  Object.assign(allocationForm, {
    assetId: row.id,
    applyUserId: 1,
    applyType: 'receive',
    assignUserId: null,
    assignDeptId: null,
    applyReason: '',
    expectedReturnDate: ''
  })
  allocationDialogVisible.value = true
}

const submitAllocation = async () => {
  if (!allocationFormRef.value) return
  await allocationFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createAllocation(allocationForm)
        ElMessage.success('分配申请已提交')
        allocationDialogVisible.value = false
        loadData()
      } catch {
        ElMessage.error('提交失败')
      }
    }
  })
}

const openMaintenanceDialog = (row) => {
  Object.assign(maintenanceForm, {
    assetId: row.id,
    reportUserId: 1,
    faultDescription: '',
    faultLocation: ''
  })
  maintenanceDialogVisible.value = true
}

const submitMaintenance = async () => {
  if (!maintenanceFormRef.value) return
  await maintenanceFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createMaintenance(maintenanceForm)
        ElMessage.success('报修申请已提交')
        maintenanceDialogVisible.value = false
        loadData()
      } catch {
        ElMessage.error('提交失败')
      }
    }
  })
}

const viewDetail = (row) => {
  detailData.value = row
  detailDialogVisible.value = true
}

const handleScrap = (row) => {
  ElMessageBox.confirm(`确定要报废资产"${row.assetName}"吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await scrapAsset(row.id)
      ElMessage.success('报废成功')
      loadData()
    } catch {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
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

const getStatusType = (status) => {
  const map = {
    'in_stock': 'success',
    'assigned': 'warning',
    'in_use': 'primary',
    'maintenance': 'danger',
    'scrapped': 'info'
  }
  return map[status] || ''
}

onMounted(() => {
  loadCategoryList()
  loadUserList()
  loadDeptList()
  loadData()
})
</script>

<style scoped>
.assets-page {
  padding-bottom: 20px;
}

.search-card {
  margin-bottom: 0;
}

.search-form {
  margin-bottom: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
