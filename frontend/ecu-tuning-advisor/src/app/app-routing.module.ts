import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { WorkshopComponent } from './components/workshop/workshop.component';
import { HealthCheckComponent } from './components/health-check/health-check.component';
import { DynoTestComponent } from './components/dyno-test/dyno-test.component';

const routes: Routes = [
  { path: '', redirectTo: 'workshop', pathMatch: 'full' },
  { path: 'workshop', component: WorkshopComponent },
  { path: 'health-check', component: HealthCheckComponent },
  { path: 'dyno-test', component: DynoTestComponent },
  { path: '**', redirectTo: 'workshop' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}