import { Component, Inject, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';

@Component({
  selector: 'app-registrar-pagamento-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Registrar Pagamento</h2>
    <mat-dialog-content>
      <p class="subtitle">Cliente: <strong>{{ data.nomeCliente }}</strong></p>
      
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Valor (R$)</mat-label>
          <input matInput type="number" formControlName="valor" id="registrar-pagamento-valor" placeholder="0.00" min="0.01" step="0.01">
          <mat-hint>O valor será abatido do saldo devedor atual.</mat-hint>
          @if (form.controls.valor.hasError('required')) {
            <mat-error>Valor é obrigatório</mat-error>
          } @else if (form.controls.valor.hasError('min')) {
            <mat-error>O valor deve ser maior que zero</mat-error>
          }
        </mat-form-field>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || salvando()" (click)="salvar()">
        @if (salvando()) { <mat-spinner diameter="18"></mat-spinner> }
        Confirmar Pagamento
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.form-grid { display: flex; flex-direction: column; gap: 4px; padding-top: 16px; min-width: 320px; }
            .subtitle { margin-top: -8px; margin-bottom: 8px; color: #616161; }
            .full-width { width: 100%; }
            .erro { color: #c62828; font-size: 13px; padding: 4px 0; }`],
})
export class RegistrarPagamentoDialogComponent {
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor(
    private clientesService: ClientesService,
    private dialogRef: MatDialogRef<RegistrarPagamentoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { clienteId: string, nomeCliente: string, saldoAtual: number }
  ) {
      if(this.data.saldoAtual) {
          this.form.patchValue({ valor: this.data.saldoAtual });
      }
  }

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const { valor } = this.form.getRawValue();
    
    this.clientesService.registrarPagamento(this.data.clienteId, { valor: Number(valor) }).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao registrar pagamento. Tente novamente.');
      },
    });
  }
}
