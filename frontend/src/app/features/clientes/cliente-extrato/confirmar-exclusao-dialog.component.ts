import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';

@Component({
  selector: 'app-confirmar-exclusao-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatButtonModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <!-- Cabeçalho com ícone de alerta -->
    <div class="dialog-header">
      <div class="danger-icon-wrapper">
        <mat-icon class="danger-icon">warning</mat-icon>
      </div>
      <h2 mat-dialog-title>Excluir cliente permanentemente</h2>
    </div>

    <mat-dialog-content>
      <!-- Bloco de aviso estilo GitHub -->
      <div class="warning-box">
        <mat-icon class="warning-box-icon">info</mat-icon>
        <div class="warning-box-text">
          <strong>Esta ação apagará os dados pessoais.</strong>
          <p>Para cumprir a LGPD, os dados de <strong class="nome-destaque">{{ data.nomeCliente }}</strong> (Nome, CPF, Telefone) serão <b>anonimizados</b>. O histórico financeiro será mantido de forma anônima e irreversível.</p>
        </div>
      </div>

      <!-- Instrução de confirmação estilo GitHub -->
      <p class="confirm-instrucao">
        Para confirmar, digite o nome do cliente abaixo:
      </p>
      <p class="nome-exibir"><strong>{{ data.nomeCliente }}</strong></p>

      <mat-form-field appearance="outline" class="confirm-field">
        <input
          matInput
          id="input-confirmar-nome"
          [formControl]="confirmCtrl"
          placeholder="{{ data.nomeCliente }}"
          autocomplete="off"
          (input)="verificarNome()"
        >
      </mat-form-field>

      @if (erro()) {
        <div class="erro-msg">
          <mat-icon class="erro-icon">error</mat-icon>
          {{ erro() }}
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions class="dialog-actions">
      <button mat-stroked-button mat-dialog-close id="btn-cancelar-exclusao" [disabled]="excluindo()">
        Cancelar
      </button>
      <button
        mat-raised-button
        id="btn-confirmar-exclusao"
        class="btn-excluir"
        [disabled]="!nomeConfirmado() || excluindo()"
        (click)="confirmar()"
      >
        @if (excluindo()) {
          <mat-spinner diameter="18" class="spinner-btn"></mat-spinner>
        } @else {
          <mat-icon>delete_forever</mat-icon>
        }
        Excluir este cliente
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 16px 0;
    }

    .danger-icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #fef2f2;
      flex-shrink: 0;
    }

    .danger-icon {
      color: #b91c1c;
      font-size: 22px;
      width: 22px;
      height: 22px;
    }

    h2[mat-dialog-title] {
      margin: 0 !important;
      font-size: 18px;
      font-weight: 600;
      color: #111827;
    }

    mat-dialog-content {
      padding: 16px 16px 0 !important;
      /* SEM min-width: deixa o dialog ser responsivo no mobile */
      max-width: 100%;
      box-sizing: border-box;
    }

    .warning-box {
      display: flex;
      gap: 12px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      padding: 12px 14px;
      margin-bottom: 20px;
    }

    .warning-box-icon {
      color: #b91c1c;
      font-size: 20px;
      width: 20px;
      height: 20px;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .warning-box-text {
      font-size: 13px;
      line-height: 1.5;
      color: #7f1d1d;
    }

    .warning-box-text strong {
      display: block;
      margin-bottom: 4px;
      color: #991b1b;
    }

    .warning-box-text p {
      margin: 0;
    }

    .nome-destaque {
      color: #7f1d1d;
    }

    .confirm-instrucao {
      font-size: 13px;
      color: #374151;
      margin-bottom: 4px;
    }

    .nome-exibir {
      font-size: 13px;
      background: #f3f4f6;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      padding: 6px 10px;
      margin-bottom: 10px;
      font-family: monospace;
      color: #111827;
      word-break: break-word;
    }

    .confirm-field {
      width: 100%;
    }

    .erro-msg {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #b91c1c;
      margin-top: -8px;
      margin-bottom: 8px;
    }

    .erro-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .btn-excluir {
      background-color: #dc2626 !important;
      color: #fff !important;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .btn-excluir:disabled {
      background-color: #f3f4f6 !important;
      color: #9ca3af !important;
    }

    .btn-excluir mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .spinner-btn {
      display: inline-block;
    }

    .dialog-actions {
      display: flex;
      gap: 8px;
      padding: 8px 16px 16px !important;
      width: 100%;
      box-sizing: border-box;
    }

    .dialog-actions button {
      flex: 1;
      min-height: 48px;
      margin: 0 !important;
    }
  `],
})
export class ConfirmarExclusaoDialogComponent {
  readonly data = inject<{ clienteId: string; nomeCliente: string }>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<ConfirmarExclusaoDialogComponent>>(MatDialogRef);
  private clientesService = inject(ClientesService);

  confirmCtrl = this.fb.control('');
  nomeConfirmado = signal(false);
  excluindo = signal(false);
  erro = signal<string | null>(null);

  verificarNome() {
    const valor = this.confirmCtrl.value ?? '';
    this.nomeConfirmado.set(valor.trim() === this.data.nomeCliente.trim());
  }

  confirmar() {
    if (!this.nomeConfirmado() || this.excluindo()) return;
    this.excluindo.set(true);
    this.erro.set(null);

    this.clientesService.excluir(this.data.clienteId).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.excluindo.set(false);
        this.erro.set('Erro ao excluir o cliente. Tente novamente.');
      },
    });
  }
}
