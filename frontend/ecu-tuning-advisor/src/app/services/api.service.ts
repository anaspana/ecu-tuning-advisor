import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Request modeli
export interface DiagnosisRequest {
  vehicleId: string;
  ecuType: string;
  vehicleBrand: string;
  mileage: number;
  maxGearboxTorque: number;
  dtcCodes: string[];
}

export interface AccumulateRequest {
  vehicleId: string;
  engineLoadHistory: number[];
}

export interface TuningValidateRequest {
  vehicleId: string;
  ecuType: string;
  brand: string;
  tuningStage: string;
  requestedBoost: number;
  requestedTemp: number;
  requestedTorque: number;
  vehicleComponents: string[];
}

export interface TelemetryEventRequest {
  vehicleId: string;
  exhaustTempCelsius?: number;
  oilPressureBar?: number;
  rpm?: number;
}

// Response modeli
export interface DiagnosticResult {
  componentStatus: string;  // ComponentStatus enum: EGR_CLOGGED, DPF_ASH_OVERLOADED...
  vehicleId: string;
}

export interface TuningAction {
  actionType: string;   // npr. EGR_OFF_RECOMMENDED, DPF_OFF_RECOMMENDED
  description: string;  // opis za korisnika
  vehicleId: string;
}

export interface ToolRecommendation {
  actionType: string;   // akcija za koju je alat preporucen
  toolName: string;     // naziv alata (PCMflash, KESSv2, daVinci...)
  protocol: string;     // nacin rada (Bench mode, OBD port...)
  notes: string;        // dodatne napomene
  vehicleId: string;
}

export interface DiagnosisResponse {
  vehicleId: string;
  diagnosticResults: DiagnosticResult[];
  tuningActions: TuningAction[];
  toolRecommendations: ToolRecommendation[];
}

export interface AccumulateResponse {
  vehicleId: string;
  averageLoadPercent: number;
  wearLevel: 'HIGH_WEAR' | 'NORMAL_WEAR';
  tuningBlocked: boolean;
  message: string;
}

export interface MissingComponent {
  vehicleId: string;
  componentName: string;
  requiredForStage: string;
}

export interface TuningDecision {
  vehicleId: string;
  decision: 'ALLOW' | 'BLOCK';
  reason: string;
  profileKey: string;
}

export interface TuningValidateResponse {
  vehicleId: string;
  finalDecision: 'ALLOW' | 'BLOCK';
  templateDecision: TuningDecision;
  missingComponents: MissingComponent[];
  messages: string[];
}

export interface TelemetryStatusResponse {
  vehicleId: string;
  safeModeActive: boolean;
  highTempWarningCount: number;
  oilPressureCritical: boolean;
  notifications: string[];
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Ekran 1, Radionica
  diagnose(req: DiagnosisRequest): Observable<DiagnosisResponse> {
    return this.http.post<DiagnosisResponse>(`${this.base}/diagnosis`, req);
  }

  validateTuning(req: TuningValidateRequest): Observable<TuningValidateResponse> {
    return this.http.post<TuningValidateResponse>(`${this.base}/tuning/validate`, req);
  }

  //Ekran 2, Health Check
  analyzeHistory(req: AccumulateRequest): Observable<AccumulateResponse> {
    return this.http.post<AccumulateResponse>(`${this.base}/diagnosis/history`, req);
  }

  // Ekran 3, Dyno Test
  sendTelemetryEvent(req: TelemetryEventRequest): Observable<TelemetryStatusResponse> {
    return this.http.post<TelemetryStatusResponse>(`${this.base}/telemetry/event`, req);
  }

  getTelemetryStatus(vehicleId: string): Observable<TelemetryStatusResponse> {
    return this.http.get<TelemetryStatusResponse>(`${this.base}/telemetry/status/${vehicleId}`);
  }

  resetTelemetry(vehicleId?: string): Observable<any> {
    return this.http.post(`${this.base}/telemetry/reset`, { vehicleId: vehicleId ?? null });
  }
}