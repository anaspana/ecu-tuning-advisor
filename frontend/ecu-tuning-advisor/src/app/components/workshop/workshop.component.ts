import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import {
  ApiService,
  DiagnosisRequest, DiagnosisResponse,
  TuningValidateRequest, TuningValidateResponse,
} from '../../services/api.service';

@Component({
  selector: 'app-workshop',
  templateUrl: './workshop.component.html',
  styleUrls: ['./workshop.component.scss'],
})
export class WorkshopComponent {

  // forma
  vehicleId   = 'VW-001';
  selectedEcu: string = '';
  vehicleBrand = '';

  // Mape ECU -> dostupne marke (prema CSV tabeli)
  brandsByEcu: Record<string, string[]> = {
    'BOSCH_EDC17':   ['VW', 'BMW', 'Audi'],
    'BOSCH_EDC16':   ['VW', 'BMW'],
    'SIEMENS_SID807':['Peugeot', 'Ford'],
  };

  get brandOptions(): string[] {
    return this.selectedEcu ? (this.brandsByEcu[this.selectedEcu] ?? []) : [];
  }
  mileage     = 180000;
  maxTorque   = 400;
  dtcCodes: string[] = [];
  dtcInput    = '';

  tuningStage: string = '';
  requestedBoost   = 1.8;
  requestedTemp    = 840;
  requestedTorque  = 380;
  vehicleComponents: string[] = [];

  // Stanje
  loadingDiagnosis = false;
  loadingTuning    = false;
  diagnosisResult: DiagnosisResponse | null = null;
  tuningResult: TuningValidateResponse | null = null;

  // Dropdown opcije
  ecuOptions = [
    { label: 'Bosch EDC17', value: 'BOSCH_EDC17' },
    { label: 'Bosch EDC16', value: 'BOSCH_EDC16' },
    { label: 'Siemens SID807', value: 'SIEMENS_SID807' },
  ];

  tuningStageOptions = [
    { label: 'Stage1 - Blago povećanje', value: 'Stage1' },
    { label: 'Stage2 - Agresivni setup',  value: 'Stage2' },
    { label: 'Eco - Smanjenje potrošnje', value: 'Eco' },
    { label: 'Stage2Tuning - Hardverski paket', value: 'Stage2Tuning' },
    { label: 'HighPerformanceTuning - Maksimalni nivo', value: 'HighPerformanceTuning' },
  ];

  componentOptions = [
    { label: 'HybridTurbine', value: 'HybridTurbine' },
    { label: 'Downpipe',      value: 'Downpipe' },
    { label: 'Intercooler',   value: 'Intercooler' },
    { label: 'Stage2Tuning',  value: 'Stage2Tuning' },
  ];

  knownDtcCodes = ['P0401', 'P0402', 'P242F', 'P2002'];

  constructor(private api: ApiService, private msg: MessageService) {}

  onEcuChange() {
    // reset marku kad se promeni ECU jer opcije se menjaju
    this.vehicleBrand = '';
  }

  addDtc(code: string) {
    const trimmed = code.trim().toUpperCase();
    if (trimmed && !this.dtcCodes.includes(trimmed)) {
      this.dtcCodes = [...this.dtcCodes, trimmed];
    }
    this.dtcInput = '';
  }

  removeDtc(code: string) {
    this.dtcCodes = this.dtcCodes.filter(c => c !== code);
  }

  runDiagnosis() {
    if (!this.selectedEcu || !this.vehicleBrand || !this.vehicleId) {
      this.msg.add({ severity: 'warn', summary: 'Nedostaju podaci', detail: 'Popuni ECU tip, marku i ID vozila.' });
      return;
    }
    this.loadingDiagnosis = true;
    this.diagnosisResult  = null;

    const req: DiagnosisRequest = {
      vehicleId:       this.vehicleId,
      ecuType:         this.selectedEcu,
      vehicleBrand:    this.vehicleBrand,
      mileage:         this.mileage,
      maxGearboxTorque:this.maxTorque,
      dtcCodes:        this.dtcCodes,
    };

    this.api.diagnose(req).subscribe({
      next: res => {
        this.diagnosisResult = res;
        this.loadingDiagnosis = false;
        this.msg.add({ severity: 'success', summary: 'Dijagnostika završena', detail: `${res.diagnosticResults?.length ?? 0} rezultata` });
      },
      error: () => {
        this.loadingDiagnosis = false;
        this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' });
      }
    });
  }

  validateTuning() {
    if (!this.selectedEcu || !this.tuningStage) {
      this.msg.add({ severity: 'warn', summary: 'Nedostaju podaci', detail: 'Odaberi ECU i nivo tuninga.' });
      return;
    }
    this.loadingTuning = true;
    this.tuningResult  = null;

    const req: TuningValidateRequest = {
      vehicleId:         this.vehicleId,
      ecuType:           this.selectedEcu,
      brand:             this.vehicleBrand,
      tuningStage:       this.tuningStage,
      requestedBoost:    this.requestedBoost,
      requestedTemp:     this.requestedTemp,
      requestedTorque:   this.requestedTorque,
      vehicleComponents: this.vehicleComponents,
    };

    this.api.validateTuning(req).subscribe({
      next: res => {
        this.tuningResult = res;
        this.loadingTuning = false;
        const sev = res.finalDecision === 'ALLOW' ? 'success' : 'error';
        this.msg.add({ severity: sev, summary: res.finalDecision === 'ALLOW' ? 'Tuning odobren!' : 'Tuning blokiran!', detail: res.templateDecision?.reason });
      },
      error: () => {
        this.loadingTuning = false;
        this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' });
      }
    });
  }

  get hasResults(): boolean {
    return !!this.diagnosisResult || !!this.tuningResult;
  }

  // enum vrednost u citljiv naziv za prikaz korisniku
  formatComponentStatus(status: string): string {
    const map: Record<string, string> = {
      'EGR_CLOGGED':           'EGR ventil - zapušen',
      'EGR_EXCESSIVE_FLOW':    'EGR ventil - prekomerni protok',
      'DPF_ASH_OVERLOADED':    'DPF filter - zasićen pepelom',
      'DPF_LOW_EFFICIENCY':    'DPF filter - niska efikasnost',
    };
    return map[status] ?? status;
  }

  dtcSeverity(status: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' {
    const s = status?.toUpperCase();
    if (s?.includes('CLOGGED') || s?.includes('CRITICAL') || s?.includes('OVERLOAD')) return 'danger';
    if (s?.includes('LOW') || s?.includes('EXCESSIVE')) return 'warning';
    return 'info';
  }
}