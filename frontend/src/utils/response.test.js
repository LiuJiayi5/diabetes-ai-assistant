import { describe, expect, it } from 'vitest'
import { assertSuccess, unwrapData } from './response'

describe('response utils', () => {
  it('unwrapData prefers nested data field', () => {
    expect(unwrapData({ data: { id: 1 } })).toEqual({ id: 1 })
    expect(unwrapData({ id: 2 })).toEqual({ id: 2 })
  })

  it('assertSuccess returns data when code is 200', () => {
    expect(assertSuccess({ code: 200, data: { ok: true } })).toEqual({ ok: true })
  })

  it('assertSuccess throws for non-200 responses', () => {
    expect(() => assertSuccess({ code: 400, message: 'bad request' })).toThrow('bad request')
    expect(() => assertSuccess(null)).toThrow('请求失败')
  })
})
