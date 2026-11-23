import { Routes } from '@angular/router';
import { authGuard, counterGuard } from './auth.guard';
import { LoginComponent } from './login/login.component';
import { ParkingListComponent } from './parking-list/parking-list.component';
import { LayoutEditorComponent } from './layout-editor/layout-editor.component';
import { CounterComponent } from './counter/counter.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: ParkingListComponent, canActivate: [authGuard] },
  { path: 'parking-areas/:id', component: LayoutEditorComponent, canActivate: [authGuard] },
  { path: 'counter', component: CounterComponent, canActivate: [counterGuard] },
  { path: '**', redirectTo: '' },
];
