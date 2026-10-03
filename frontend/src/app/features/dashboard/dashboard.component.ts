import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';
import { NovoFuncionarioDialogComponent } from './novo-funcionario-dialog.component';
import { FuncionariosService, ResumoFarmacia } from './funcionarios.service';
import { CurrencyBrPipe } from '../../shared/pipes/currency-br.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, MatToolbarModule, MatIconModule, MatButtonModule,
    MatCardModule, MatDialogModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatTooltipModule, CurrencyBrPipe,
  ],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <mat-icon class="toolbar-logo">dashboard</mat-icon>
      <span class="toolbar-title">Dashboard Gerencial</span>
      <span class="spacer"></span>
      <span class="usuario-nome">{{ auth.usuario()?.nome }}</span>
      <button mat-icon-button (click)="voltar()" aria-label="Voltar para clientes" matTooltip="Clientes">
        <mat-icon>people</mat-icon>
      </button>
      <button mat-icon-button (click)="logout()" aria-label="Sair" matTooltip="Sair">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    <div class="page-container">
      <div class="header-actions">
        <h2 class="title">Visão Geral</h2>
        <div class="header-buttons">
          <button mat-stroked-button class="btn-atualizar" (click)="carregarResumo()"
                  [disabled]="carregando()" matTooltip="Atualizar dados">
            <mat-icon>refresh</mat-icon>
          </button>
          <button mat-flat-button color="primary" (click)="abrirNovoFuncionario()" aria-label="Novo Funcionário">
            <mat-icon>person_add</mat-icon>
            Novo Funcionário
          </button>
        </div>
      </div>

      <!-- Cards de métricas -->
      <div class="dashboard-grid">

        <!-- Card: Total a Receber -->
        <mat-card class="dash-card card-receber">
          <mat-card-content class="dash-card-content">
            <div class="card-icon-wrapper receber-icon">
              <mat-icon>attach_money</mat-icon>
            </div>
            <div class="card-info">
              <p class="card-label">Total a Receber</p>
              @if (carregando()) {
                <mat-spinner diameter="32" class="card-spinner"></mat-spinner>
              } @else if (erro()) {
                <span class="card-erro">—</span>
              } @else {
                <h2 class="dash-value receber-value">{{ resumo()?.totalAReceber | currencyBr }}</h2>
              }
              <p class="dash-desc">Soma dos saldos devedores de todos os clientes</p>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Card: Clientes com Débito -->
        <mat-card class="dash-card card-ativos">
          <mat-card-content class="dash-card-content">
            <div class="card-icon-wrapper ativos-icon">
              <mat-icon>group</mat-icon>
            </div>
            <div class="card-info">
              <p class="card-label">Clientes com Débito</p>
              @if (carregando()) {
                <mat-spinner diameter="32" class="card-spinner"></mat-spinner>
              } @else if (erro()) {
                <span class="card-erro">—</span>
              } @else {
                <h2 class="dash-value ativos-value">{{ resumo()?.clientesAtivos }}</h2>
              }
              <p class="dash-desc">Clientes com saldo devedor em aberto</p>
            </div>
          </mat-card-content>
        </mat-card>

      </div>

      @if (erro()) {
        <div class="erro-box">
          <mat-icon>error_outline</mat-icon>
          <span>Não foi possível carregar os dados. Tente atualizar.</span>
        </div>
      }

    </div>
  `,
  styles: [`
    .toolbar {
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .toolbar .toolbar-logo { margin-right: 8px; }
    .toolbar .toolbar-title { font-weight: 700; font-size: 18px; }
    .toolbar .spacer { flex: 1; }
    .toolbar .usuario-nome { font-size: 14px; opacity: 0.85; margin-right: 8px; }

    .page-container {
      max-width: 960px;
      margin: 0 auto;
      padding: 16px 12px;
      box-sizing: border-box;
    }

    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      flex-wrap: wrap;
      gap: 12px;
    }

    .header-buttons {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .btn-atualizar {
      min-width: 40px !important;
      width: 40px;
      height: 40px;
      border-radius: 50% !important;
      padding: 0;
    }

    .title {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
      margin: 0;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
      margin-bottom: 20px;
    }

    .dash-card {
      border-radius: 16px !important;
      border: 1.5px solid transparent;
      transition: box-shadow 0.2s, transform 0.2s;
    }
    .dash-card:hover {
      box-shadow: 0 8px 24px rgba(0,0,0,0.12) !important;
      transform: translateY(-2px);
    }
    .dash-card.card-receber {
      background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%) !important;
      border-color: #a5d6a7;
    }
    .dash-card.card-ativos {
      background: linear-gradient(135deg, #e3f2fd 0%, #e8eaf6 100%) !important;
      border-color: #90caf9;
    }

    .dash-card-content {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px 18px !important;
    }

    .card-icon-wrapper {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .card-icon-wrapper mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }
    .card-icon-wrapper.receber-icon { background: #c8e6c9; }
    .card-icon-wrapper.receber-icon mat-icon { color: #2e7d32; }
    .card-icon-wrapper.ativos-icon { background: #bbdefb; }
    .card-icon-wrapper.ativos-icon mat-icon { color: #1565c0; }

    .card-info { flex: 1; min-width: 0; }

    .card-label {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: #757575;
      margin: 0 0 4px;
    }

    .dash-value {
      font-size: 34px;
      font-weight: 800;
      margin: 0 0 4px;
      line-height: 1.1;
    }
    .dash-value.receber-value { color: #1b5e20; }
    .dash-value.ativos-value  { color: #0d47a1; }

    .dash-desc {
      font-size: 12px;
      color: #9e9e9e;
      margin: 0;
    }

    .card-spinner { margin: 4px 0; }

    .card-erro {
      font-size: 28px;
      font-weight: 700;
      color: #bdbdbd;
    }

    .erro-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #fff3f3;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #b91c1c;
      font-size: 14px;
    }
    .erro-box mat-icon { font-size: 20px; width: 20px; height: 20px; }
  `]
})
export class DashboardComponent implements OnInit {
  resumo = signal<ResumoFarmacia | null>(null);
  carregando = signal(true);
  erro = signal(false);

  constructor(
    public auth: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private funcionariosService: FuncionariosService,
  ) { }

  ngOnInit() {
    this.carregarResumo();
  }

  carregarResumo() {
    this.carregando.set(true);
    this.erro.set(false);
    this.funcionariosService.resumo().subscribe({
      next: dados => {
        this.resumo.set(dados);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  logout() {
    this.auth.logout();
  }

  voltar() {
    this.router.navigate(['/clientes']);
  }

  abrirNovoFuncionario() {
    const ref = this.dialog.open(NovoFuncionarioDialogComponent, { 
      width: '92vw', maxWidth: '480px', disableClose: false 
    });
    ref.afterClosed().subscribe(criado => {
      if (criado) {
        this.snackBar.open('Funcionário cadastrado com sucesso!', 'Fechar', { duration: 3000 });
      }
    });
  }
}
