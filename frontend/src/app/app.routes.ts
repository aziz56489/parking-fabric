import { Routes } from '@angular/router';
import { ParkingListComponent } from './parking-list/parking-list.component';
import { LayoutEditorComponent } from './layout-editor/layout-editor.component';

export const routes: Routes = [
  { path: '', component: ParkingListComponent },
  { path: 'parking-areas/:id', component: LayoutEditorComponent },
];
