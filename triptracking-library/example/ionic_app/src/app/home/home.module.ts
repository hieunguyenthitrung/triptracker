import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { HomePage } from './home.page';

@NgModule({
  imports: [
    HomePage,
    RouterModule.forChild([{ path: '', component: HomePage }])
  ]
})
export class HomePageModule {}
