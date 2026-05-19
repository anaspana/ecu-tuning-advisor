import { Component, OnDestroy } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ApiService, TelemetryStatusResponse } from '../../services/api.service';

interface SensorEvent {
  time: string;
  type: string;
  value: string;
  status: 'warning' | 'critical' | 'normal';
}

@Component({
  selector: 'app-dyno-test',
  templateUrl: './dyno-test.component.html',
  styleUrls: ['./dyno-test.component.scss'],
})
export class DynoTestComponent implements OnDestroy {

  vehicleId = 'VW-001';

  // Forma za slanje događaja
  exhaustTemp: number = 820;
  oilPressure: number = 2.5;
  rpmTemp: number = 2000;
  rpmOil: number  = 2000;  
  sendingTemp = false;
  sendingOil  = false;

  // Status
  status: TelemetryStatusResponse | null = null;
  eventLog: SensorEvent[] = [];

  // Prati stanje da ne ponavljamo toast popupe
  private _lastWarningCount    = 0;
  private _safeModeNotified    = false;
  private _oilCriticalNotified = false;

  // Preset scenariji za demonstraciju
  tempPresets = [
    { label: 'Normalna temp',  icon: 'pi-check-circle', temp: 820,  rpm: 2000, description: 'Ispod limita 850°C' },
    { label: 'Blagi warning',  icon: 'pi-exclamation-triangle', temp: 855, rpm: 2500, description: 'Malo iznad limita' },
    { label: 'Visoka temp',    icon: 'pi-bolt',          temp: 875,  rpm: 3000, description: 'Jasno iznad limita - generiše Warning' },
    { label: 'Kritična temp',  icon: 'pi-exclamation-circle',          temp: 900,  rpm: 3500, description: 'Agresivno prekoračenje' },
  ];

  oilPresets = [
    { label: 'Normalan pritisak', icon: 'pi-check-circle', pressure: 2.5, rpm: 2000, description: 'OK' },
    { label: 'Nizak pritisak',    icon: 'pi-bolt',          pressure: 0.9, rpm: 2800, description: 'Ispod 1.2 bara @ >2500 RPM → kritično' },
  ];

  constructor(private api: ApiService, private msg: MessageService) {}

  ngOnDestroy() {}

  applyTempPreset(p: any) {
    this.exhaustTemp = p.temp;
    this.rpmTemp = p.rpm;
  }

  applyOilPreset(p: any) {
    this.oilPressure = p.pressure;
    this.rpmOil = p.rpm;
  }

  sendTempEvent() {
    this.sendingTemp = true;
    this.api.sendTelemetryEvent({
      vehicleId: this.vehicleId,
      exhaustTempCelsius: this.exhaustTemp,
      rpm: this.rpmTemp,
    }).subscribe({
      next: res => {
        this.status = res;
        this.sendingTemp = false;

        // odredjujemo status TRENUTNOG dogadjaja za Event Log
        let eventStatus: 'normal' | 'warning' | 'critical' = 'normal';
        if (res.safeModeActive && !this._safeModeNotified) eventStatus = 'critical'; // samo event koji je aktivirao Safe Mode
        else if (this.exhaustTemp > 850) eventStatus = 'warning';
        else eventStatus = 'normal'; // normalna temp ostaje normalna cak i posle Safe Mode
 
        this.logEvent('TEMP', `${this.exhaustTemp}°C @ ${this.rpmTemp} RPM`, eventStatus);
 
        // toast prikazujemo SAMO ako je trenutni dogadjaj problematican
        if (res.safeModeActive && !this._safeModeNotified) {
          this._safeModeNotified = true;
          this.msg.add({ severity: 'error', summary: '🚨 SAFE MODE AKTIVAN!', detail: 'Pritisak turbine automatski oboren.', life: 6000 });
        } else if (!res.safeModeActive && this.exhaustTemp > 850 && res.highTempWarningCount > this._lastWarningCount) {
          this._lastWarningCount = res.highTempWarningCount;
          const preostalo = Math.max(0, 4 - res.highTempWarningCount);
          this.msg.add({ severity: 'warn', summary: `Warning #${res.highTempWarningCount}`, detail: preostalo > 0 ? `Temperatura prelazi limit. Jos ${preostalo} do Safe Mode.` : 'Sledece prekoracenje aktivira Safe Mode!', life: 3000 });
        }
      },
      error: () => {
        this.sendingTemp = false;
        this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' });
      }
    });
  }

  sendOilEvent() {
    this.sendingOil = true;
    this.api.sendTelemetryEvent({
      vehicleId: this.vehicleId,
      oilPressureBar: this.oilPressure,
      rpm: this.rpmOil,
    }).subscribe({
      next: res => {
        this.status = res;
        this.sendingOil = false;
 
        // Odredjujemo status za Event log na osnovu TRENUTNIH vrednosti(ne globalnog flaga
        let eventStatus: 'normal' | 'warning' | 'critical' = 'normal';
        if (res.oilPressureCritical && !this._oilCriticalNotified) eventStatus = 'critical'; // samo event koji je aktivirao critical
        else if (this.oilPressure < 1.2 && this.rpmOil > 2500) eventStatus = 'warning';
        else eventStatus = 'normal';
 
        this.logEvent('OIL', `${this.oilPressure} bar @ ${this.rpmOil} RPM`, eventStatus);
 
        if (res.oilPressureCritical && !this._oilCriticalNotified) {
          this._oilCriticalNotified = true;
          this.msg.add({ severity: 'error', summary: '🚨 KRITIČAN PRITISAK ULJA!', detail: 'EngineCriticalEvent aktiviran.', life: 6000 });
        }
      },
      error: () => {
        this.sendingOil = false;
        this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' });
      }
    });
  }

  // U logEvent saljemo izracunati status
  private logEvent(type: string, value: string, status: 'warning' | 'critical' | 'normal') {
    const now = new Date();
    const time = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}:${now.getSeconds().toString().padStart(2,'0')}`;
    
    this.eventLog = [{ time, type, value, status }, ...this.eventLog.slice(0, 19)];
  }

  reset() {
    this.api.resetTelemetry(this.vehicleId).subscribe({
      next: () => {
        this.status   = null;
        this.eventLog = [];
        this._lastWarningCount    = 0;
        this._safeModeNotified    = false;
        this._oilCriticalNotified = false;
        this.msg.add({ severity: 'info', summary: 'Reset', detail: 'CEP sesija ociscena.' });
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Greška', detail: 'Backend nije dostupan.' })
    });
  }

  get safeMode(): boolean { return this.status?.safeModeActive ?? false; }
  get warningCount(): number { return this.status?.highTempWarningCount ?? 0; }
  get oilCritical(): boolean { return this.status?.oilPressureCritical ?? false; }

  get warningProgress(): number {
    return Math.min(100, (this.warningCount / 4) * 100);
  }
  get warningProgressColor(): string {
    if (this.warningCount >= 4) return 'var(--accent-red)';
    if (this.warningCount >= 2) return 'var(--accent-orange)';
    return 'var(--accent-blue)';
  }
}