import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatIconModule, MatButtonModule, MatCardModule],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <mat-icon class="toolbar-logo">dashboard</mat-icon>
      <span class="toolbar-title">Dashboard Gerencial</span>
      <span class="spacer"></span>
      <span class="usuario-nome">{{ auth.usuario()?.nome }}</span>
      <button mat-icon-button (click)="voltar()" aria-label="Voltar para clientes">
        <mat-icon>people</mat-icon>
      </button>
      <button mat-icon-button (click)="logout()" aria-label="Sair">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    <div class="page-container">
      <h2 class="title">Visão Geral</h2>
      <div class="dashboard-grid">
        <mat-card class="dash-card">
          <mat-card-header>
            <mat-icon mat-card-avatar color="primary">attach_money</mat-icon>
            <mat-card-title>A Receber</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <h1 class="dash-value">Em breve</h1>
            <p class="dash-desc">Total de saldos devedores (Sprint 2)</p>
          </mat-card-content>
        </mat-card>

        <mat-card class="dash-card">
          <mat-card-header>
            <mat-icon mat-card-avatar color="accent">group</mat-icon>
            <mat-card-title>Clientes Ativos</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <h1 class="dash-value">Em breve</h1>
            <p class="dash-desc">Clientes com pendências (Sprint 2)</p>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .toolbar {
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
      position: sticky;
      top: 0;
      z-index: 100;
      .toolbar-logo { margin-right: 8px; }
      .toolbar-title { font-weight: 700; font-size: 18px; }
      .spacer { flex: 1; }
      .usuario-nome { font-size: 14px; opacity: 0.85; margin-right: 8px; }
    }
    .page-container {
      max-width: 960px;
      margin: 0 auto;
      padding: 24px 16px;
    }
    .title {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
      margin-bottom: 24px;
    }
    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;
    }
    .dash-card {
      border-radius: 12px !important;
      padding: 8px;
    }
    .dash-value {
      font-size: 32px;
      font-weight: 800;
      color: #424242;
      margin: 16px 0 4px;
    }
    .dash-desc {
      color: #757575;
      font-size: 14px;
      margin: 0;
    }
  `]
})
export class DashboardComponent {
  constructor(public auth: AuthService, private router: Router) {}
  
  logout() {
    this.auth.logout();
  }

  voltar() {
    this.router.navigate(['/clientes']);
  }
}
