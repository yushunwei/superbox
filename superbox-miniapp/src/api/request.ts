const BASE_URL = 'http://localhost:8080/api/v1'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  params?: Record<string, any>
  header?: Record<string, string>
}

function getToken(): string {
  return uni.getStorageSync('token') || ''
}

function buildUrl(path: string, params?: Record<string, any>): string {
  let url = BASE_URL + path
  if (params) {
    const qs = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (qs) url += '?' + qs
  }
  return url
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }
    if (options.header) {
      Object.assign(header, options.header)
    }

    uni.request({
      url: buildUrl(options.url, options.params),
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('Unauthorized'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data as T)
        } else {
          const errData = res.data as any
          reject(new Error(errData?.message || `HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || 'Network error'))
      },
    })
  })
}

export const api = {
  get: <T = any>(url: string, params?: Record<string, any>) =>
    request<T>({ url, method: 'GET', params }),

  post: <T = any>(url: string, data?: any) =>
    request<T>({ url, method: 'POST', data }),

  put: <T = any>(url: string, data?: any) =>
    request<T>({ url, method: 'PUT', data }),

  delete: <T = any>(url: string) =>
    request<T>({ url, method: 'DELETE' }),

  patch: <T = any>(url: string, data?: any) =>
    request<T>({ url, method: 'PATCH', data }),
}
