import request from '@/api/request'
import type {
  StandardQualityGateConfig,
  StandardQualityGateEvaluateReq,
  StandardQualityGateResult,
  StandardQualityGateSaveReq
} from '@/types'

export function getQualityGateConfig(projectId: number) {
  return request.get<unknown, StandardQualityGateConfig>('/quality-gate/config', {
    params: { projectId }
  })
}

export function saveQualityGateConfig(data: StandardQualityGateSaveReq) {
  return request.put<unknown, StandardQualityGateConfig>('/quality-gate/config', data)
}

export function evaluateQualityGate(data: StandardQualityGateEvaluateReq) {
  return request.post<unknown, StandardQualityGateResult>('/quality-gate/evaluate', data)
}
