import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// PrimeNG moduli
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { ChipModule } from 'primeng/chip';
import { ChipsModule } from 'primeng/chips';
import { TagModule } from 'primeng/tag';
import { BadgeModule } from 'primeng/badge';
import { ToastModule } from 'primeng/toast';
import { MessageModule } from 'primeng/message';
import { MessagesModule } from 'primeng/messages';
import { ProgressBarModule } from 'primeng/progressbar';
import { KnobModule } from 'primeng/knob';
import { DividerModule } from 'primeng/divider';
import { TimelineModule } from 'primeng/timeline';
import { TableModule } from 'primeng/table';
import { CheckboxModule } from 'primeng/checkbox';
import { MultiSelectModule } from 'primeng/multiselect';
import { TooltipModule } from 'primeng/tooltip';
import { AnimateOnScrollModule } from 'primeng/animateonscroll';
import { RippleModule } from 'primeng/ripple';
import { MessageService } from 'primeng/api';

// Stranice
import { WorkshopComponent } from './components/workshop/workshop.component';
import { HealthCheckComponent } from './components/health-check/health-check.component';
import { DynoTestComponent } from './components/dyno-test/dyno-test.component';
import { NavbarComponent } from './components/navbar/navbar.component';

@NgModule({
  declarations: [
    AppComponent,
    WorkshopComponent,
    HealthCheckComponent,
    DynoTestComponent,
    NavbarComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    InputNumberModule,
    DropdownModule,
    ChipModule,
    ChipsModule,
    TagModule,
    BadgeModule,
    ToastModule,
    MessageModule,
    MessagesModule,
    ProgressBarModule,
    KnobModule,
    DividerModule,
    TimelineModule,
    TableModule,
    CheckboxModule,
    MultiSelectModule,
    TooltipModule,
    AnimateOnScrollModule,
    RippleModule,
  ],
  providers: [MessageService],
  bootstrap: [AppComponent],
})
export class AppModule {}