import request from '@/api/request'
import type { SchemaContract, SchemaRegistryCatalog } from '@/types'

export function listContracts() {
  return request.get<unknown, SchemaRegistryCatalog>('/contracts')
}

export function getContract(contractId: string) {
  return request.get<unknown, SchemaContract>(`/contracts/${encodeURIComponent(contractId)}`)
}
