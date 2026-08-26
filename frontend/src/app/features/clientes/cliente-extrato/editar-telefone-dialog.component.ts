import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ClientesService } from '../clientes.service';
import { InputMaskDirective } from '../../../shared/directives/input-mask.directive';

@Component({
  selector: 'app-editar-telefone-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule, MatIconModule, InputMaskDirective,
  ],
  template: `
    <h2 mat-dialog-title>Editar Telefone</h2>
    <mat-dialog-content>
      <p class="subtitle">Cliente: <strong>{{ data.nomeCliente }}</strong></p>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Novo telefone</mat-label>
          <mat-icon matPrefix>phone</mat-icon>
          <input matInput formControlName="telefone" id="editar-telefone-input"
                 mask="telefone" placeholder="(11) 99999-9999" inputmode="tel">
          <mat-hint>Deixe em branco para remover o telefone.</mat-hint>
          @if (form.controls.telefone.hasError('minlength')) {
            <mat-error>Telefone incompleto</mat-error>
          }
        </mat-form-field>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close id="btn-cancelar-editar-telefone">Cancelar</button>
      <button mat-flat-button color="primary" id="btn-salvar-editar-telefone"
              [disabled]="form.invalid || salvando()" (click)="salvar()">
        @if (salvando()) { <mat-spinner diameter="18"></mat-spinner> }
        Salvar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.form-grid { display: flex; flex-direction: column; gap: 4px; padding-top: 12px; min-width: 340px; }
            .subtitle { margin-top: -8px; margin-bottom: 8px; color: #616161; }
            .full-width { width: 100%; }
            .erro { color: #c62828; font-size: 13px; padding: 4px 0; }`],
})
export class EditarTelefoneDialogComponent {
  readonly data = inject<{ clienteId: string; nomeCliente: string; telefoneAtual: string | null }>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<EditarTelefoneDialogComponent>>(MatDialogRef);
  private clientesService = inject(ClientesService);

  // Mínimo de 14 chars para "(00) 0000-0000", 15 para celular — ou vazio (campo opcional)
  form = this.fb.nonNullable.group({
    telefone: [
      this.data?.telefoneAtual ?? '',
      [Validators.minLength(14)],
    ],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor() {}

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);

    const telefoneRaw = this.form.getRawValue().telefone;
    // Se o campo está vazio, envia null para remover o telefone
    const telefone = telefoneRaw.trim() || null;

    this.clientesService.atualizarTelefone(this.data.clienteId, telefone).subscribe({
      next: (clienteAtualizado) => this.dialogRef.close(clienteAtualizado),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao salvar. Tente novamente.');
      },
    });
  }
}
