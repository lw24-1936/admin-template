import request from '@/utils/request'

/** 数据字典 API。 */
export function pageDicts(data) {
  return request.post('/sysDict/paginQuery', data)
}

export function dictItems(dictType) {
  return request.post('/sysDict/items', { dictType })
}

export function addDict(data) {
  return request.post('/sysDict/add', data)
}

export function editDict(data) {
  return request.post('/sysDict/edit', data)
}

export function deleteDict(id) {
  return request.post('/sysDict/deleteById', { id })
}
