import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  template: `
    <nav class="navbar">
      <div class="navbar-brand">
        <i class="pi pi-cog navbar-icon"></i>
        <span class="navbar-title">ECU Tuning Advisor</span>
      </div>
      <div class="navbar-links">
        <a
          *ngFor="let link of links"
          [routerLink]="link.path"
          routerLinkActive="active"
          class="nav-link"
          [pTooltip]="link.tooltip"
          tooltipPosition="bottom"
        >
          <i class="pi {{ link.icon }}"></i>
          <span>{{ link.label }}</span>
        </a>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 32px;
      height: 60px;
      background: #1a1a1a;
      border-bottom: 1px solid var(--border-accent);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .navbar-icon {
      font-size: 1.3rem;
      color: var(--accent-blue);
    }
    .navbar-title {
      font-size: 1rem;
      font-weight: 700;
      color: var(--text-primary);
      letter-spacing: 0.02em;
    }
    .navbar-links {
      display: flex;
      gap: 4px;
    }
    .nav-link {
      display: flex;
      align-items: center;
      gap: 7px;
      padding: 7px 16px;
      border-radius: 6px;
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 0.88rem;
      font-weight: 500;
      transition: background 0.15s, color 0.15s;

      i { font-size: 0.9rem; }

      &:hover {
        background: rgba(255,255,255,0.05);
        color: var(--text-primary);
      }
      &.active {
        background: rgba(77,171,247,0.12);
        color: var(--accent-blue);
      }
    }
  `]
})
export class NavbarComponent {
  links = [
    { path: '/workshop',    label: 'Radionica',       icon: 'pi-wrench',      tooltip: 'Forward Chaining + Template + Backward Chaining' },
    { path: '/health-check',label: 'Health Check',    icon: 'pi-heart',       tooltip: 'Accumulate - analiza istorije voznji' },
    { path: '/dyno-test',   label: 'Dyno Test',       icon: 'pi-chart-line',  tooltip: 'CEP - live telemetrija' },
  ];

  constructor(public router: Router) {}
}