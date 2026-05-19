import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DynoTestComponent } from './dyno-test.component';

describe('DynoTestComponent', () => {
  let component: DynoTestComponent;
  let fixture: ComponentFixture<DynoTestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DynoTestComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DynoTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
