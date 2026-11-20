import request from '@/utils/request'

/**
 * 系统参数配置 API。
 */
export function pageConfigs(data) {
  return request.post('/sysConfig/paginQuery', data)
}

export function addConfig(data) {
  return request.post('/sysConfig/add', data)
}

export function editConfig(data) {
  return request.post('/sysConfig/edit', data)
}

export function deleteConfig(id) {
  return request.post('/sysConfig/deleteById', { id })
}
