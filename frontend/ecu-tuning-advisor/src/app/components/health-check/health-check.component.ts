import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ApiService, AccumulateResponse } from '../../services/api.service';

@Component({
  selector: 'app-health-check',
  templateUrl: './health-check.component.html',
  styleUrls: ['./health-check.component.scss'],
})
export class HealthCheckComponent {

  vehicleId = 'VW-001';
  loadings: number[] = [82, 79, 85, 91, 76];
  newReading: number = 80;
  loading = false;
  result: AccumulateResponse | null = null;

  // Preset scenariji za demonstraciju
  presets = [
    { label: 'Normalna vožnja',    icon: 'pi-thumbs-up',    values: [55, 60, 58, 62, 50, 57, 63],  description: 'Prosek ~57% → NORMAL_WEAR' },
    { label: 'Agresivna vožnja',   icon: 'pi-bolt',         values: [82, 79, 85, 91, 76, 88, 84],  description: 'Prosek ~83% → HIGH_WEAR' },
    { label: 'Granična vrednost',  icon: 'pi-minus-circle', values: [75, 74, 76, 75, 74, 76, 75],  description: 'Prosek ~75% → NORMAL_WEAR (granica)' },
  ];

  constructor(private api: ApiService, private msg: MessageService) {}

  applyPreset(preset: any) {
    this.loadings = [...preset.values];
    this.result = null;
    this.msg.add({ severity: 'info', summary: 'Preset primenjen', detail: preset.description });
  }

  addReading() {
    if (this.newReading >= 0 && this.newReading <= 100) {
      this.loadings = [...this.loadings, this.newReading];
    }
  }

  removeReading(index: number) {
    this.loadings = this.loadings.filter((_, i) => i !== index);
  }

  get average(): number {
    if (!this.loadings.length) return 0;
    return Math.round(this.loadings.reduce((a, b) => a + b, 0) / this.loadings.length * 100) / 100;
  }

  analyze() {
    if (!this.loadings.length) {
      this.msg.add({ severity: 'warn', summary: 'Nedostaju podaci', detail: 'Dodaj bar jedno očitavanje.' });
      return;
    }
    this.loading = true;
    this.result  = null;

    this.api.analyzeHistory({ vehicleId: this.vehicleId, engineLoadHistory: this.loadings }).subscribe({
      next: res => {
        this.result  = res;
        this.loading = false;
        const sev = res.wearLevel === 'HIGH_WEAR' ? 'error' : 'success';
        this.msg.add({ severity: sev, summary: res.wearLevel === 'HIGH_WEAR' ? 'HIGH WEAR detektovan!' : 'Motor u dobrom stanju', detail: res.message });
      },
      error: () => {
        this.loading = false;
        this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' });
      }
    });
  }

  get progressColor(): string {
    if (this.average > 75) return 'var(--accent-red)';
    if (this.average > 60) return 'var(--accent-orange)';
    return 'var(--accent-green)';
  }
}