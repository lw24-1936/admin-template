/** 后端 LocalDateTime 序列化值（YYYY-MM-DDTHH:mm:ss[.SSS]）→ 表格展示格式。 */
export function formatDateTime(value) {
  if (value === null || value === undefined || value === '') return '—'
  const text = String(value)
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(text)) {
    return text.slice(0, 19).replace('T', ' ')
  }
  return text
}
