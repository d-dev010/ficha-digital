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
  selector: 'app-lancar-fiado-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Lançar Fiado</h2>
    <mat-dialog-content>
      <p class="subtitle">Cliente: <strong>{{ data.nomeCliente }}</strong></p>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Valor (R$)</mat-label>
          <input matInput type="number" formControlName="valor" id="lancar-fiado-valor" placeholder="0.00" min="0.01" step="0.01">
          @if (form.controls.valor.hasError('required')) {
            <mat-error>Valor é obrigatório</mat-error>
          } @else if (form.controls.valor.hasError('min')) {
            <mat-error>O valor deve ser maior que zero</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Descrição</mat-label>
          <textarea matInput formControlName="descricao" id="lancar-fiado-descricao" rows="2" placeholder="Opcional. Ex: Paracetamol, etc."></textarea>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Quem retirou? (opcional)</mat-label>
          <input matInput formControlName="pessoaRetirou" id="lancar-fiado-pessoa-retirou" placeholder="Nome de quem pegou o item">
          <mat-hint>Deixe em branco se foi o próprio cliente</mat-hint>
        </mat-form-field>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || salvando()" (click)="salvar()">
        @if (salvando()) { <mat-spinner diameter="18"></mat-spinner> }
        Confirmar Fiado
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.form-grid { display: flex; flex-direction: column; gap: 4px; padding-top: 16px; min-width: 320px; }
            .subtitle { margin-top: -8px; margin-bottom: 8px; color: #616161; }
            .full-width { width: 100%; }
            .erro { color: #c62828; font-size: 13px; padding: 4px 0; }`],
})
export class LancarFiadoDialogComponent {
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
    descricao: [''],
    pessoaRetirou: [''],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor(
    private clientesService: ClientesService,
    private dialogRef: MatDialogRef<LancarFiadoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { clienteId: string, nomeCliente: string }
  ) {}

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const { valor, descricao, pessoaRetirou } = this.form.getRawValue();
    
    this.clientesService.lancarFiado(this.data.clienteId, {
      valor: Number(valor),
      descricao,
      pessoaRetirou: pessoaRetirou || undefined,
    }).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao lançar fiado. Tente novamente.');
      },
    });
  }
}
