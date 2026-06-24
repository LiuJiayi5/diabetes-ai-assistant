export function unwrapData(response) {
  return response?.data ?? response
}

export function assertSuccess(response, fallbackMessage = '请求失败') {
  if (!response || response.code !== 200) {
    const error = new Error(response?.message || fallbackMessage)
    error.code = response?.code
    error.data = response?.data
    throw error
  }
  return response.data
}
