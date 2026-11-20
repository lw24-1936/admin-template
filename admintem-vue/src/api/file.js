import request from '@/utils/request'

/** 文件中心 API。 */
export function pageFiles(data) {
  return request.post('/sysFile/paginQuery', data)
}

export function uploadFile(file, remark = '') {
  const form = new FormData()
  form.append('file', file)
  if (remark) form.append('remark', remark)
  return request.post('/sysFile/upload', form)
}

export function deleteFile(id) {
  return request.post('/sysFile/deleteById', { id })
}
