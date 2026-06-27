import request from './request'

function unwrap(response) {
  return response?.data ?? response
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/files/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(unwrap)
}
