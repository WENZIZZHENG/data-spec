import request from '@/api/request'
import type { AiEvidencePackage, AiEvidencePackageReq } from '@/types'

export function generateEvidencePackage(data: AiEvidencePackageReq) {
  return request.post<unknown, AiEvidencePackage>('/evidence-packages', data)
}

export function downloadEvidencePackage(data: AiEvidencePackageReq) {
  return request.post<unknown, Blob>('/evidence-packages/download', data, {
    responseType: 'blob'
  })
}
