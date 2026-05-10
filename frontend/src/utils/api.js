import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const getUserInfo = () => request.get('/auth/info')
export const logout = () => request.post('/auth/logout')

export const getUsers = (params) => request.get('/common/users', { params })
export const getDepts = () => request.get('/common/depts')

export const getCategoryList = () => request.get('/asset/category/list')
export const addCategory = (data) => request.post('/asset/category', data)
export const updateCategory = (id, data) => request.put('/asset/category/' + id, data)
export const deleteCategory = (id) => request.delete('/asset/category/' + id)

export const getAssetList = (params) => request.get('/asset/list', { params })
export const getAssetDetail = (id) => request.get('/asset/' + id)
export const addAsset = (data) => request.post('/asset', data)
export const updateAsset = (id, data) => request.put('/asset/' + id, data)
export const deleteAsset = (id) => request.delete('/asset/' + id)
export const scrapAsset = (id) => request.post('/asset/' + id + '/scrap')

export const getAllocationList = (params) => request.get('/asset/allocation/list', { params })
export const createAllocation = (data) => request.post('/asset/allocation', data)
export const approveAllocation = (id, data) => request.post('/asset/allocation/' + id + '/approve', data)
export const returnAllocation = (id) => request.post('/asset/allocation/' + id + '/return')

export const getTransferList = (params) => request.get('/asset/transfer/list', { params })
export const createTransfer = (data) => request.post('/asset/transfer', data)
export const approveTransfer = (id, data) => request.post('/asset/transfer/' + id + '/approve', data)

export const getMaintenanceList = (params) => request.get('/asset/maintenance/list', { params })
export const createMaintenance = (data) => request.post('/asset/maintenance', data)
export const startMaintenance = (id) => request.post('/asset/maintenance/' + id + '/start')
export const completeMaintenance = (id, data) => request.post('/asset/maintenance/' + id + '/complete', data)

export const getDepreciationList = (params) => request.get('/asset/depreciation/list', { params })
export const calculateDepreciation = (data) => request.post('/asset/depreciation/calculate', data)

export const getDashboardData = () => request.get('/asset/dashboard')
export const getSummaryReport = () => request.get('/asset/report/summary')
export const getExpiringReport = () => request.get('/asset/report/expiring')
