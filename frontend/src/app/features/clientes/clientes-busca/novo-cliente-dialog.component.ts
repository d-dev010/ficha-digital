import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ClientesService } from '../clientes.service';
import { InputMaskDirective } from '../../../shared/directives/input-mask.directive';

@Component({
  selector: 'app-novo-cliente-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, MatCheckboxModule, InputMaskDirective,
  ],
  template: `
    <h2 mat-dialog-title>Novo Cliente</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">
        <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
          <mat-label>Nome *</mat-label>
          <input matInput formControlName="nome" id="novo-cliente-nome" placeholder="Nome completo">
          @if (form.controls.nome.hasError('required')) {
            <mat-error>Nome é obrigatório</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
          <mat-label>Telefone</mat-label>
          <input matInput formControlName="telefone" id="novo-cliente-telefone"
                 mask="telefone" placeholder="(11) 99999-9999" type="tel" inputmode="numeric">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
          <mat-label>CPF (opcional)</mat-label>
          <input matInput formControlName="cpf" id="novo-cliente-cpf"
                 mask="cpf" placeholder="000.000.000-00" inputmode="numeric" pattern="[0-9]*">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
          <mat-label>Endereço (opcional)</mat-label>
          <input matInput formControlName="endereco" id="novo-cliente-endereco" placeholder="Rua, número, bairro...">
        </mat-form-field>

        <div class="lgpd-container">
          <mat-checkbox formControlName="consentimentoLgpd" color="primary" class="lgpd-checkbox">
            O cliente está ciente e autoriza o armazenamento dos seus dados para gestão do fiado.
          </mat-checkbox>
          @if (form.controls.consentimentoLgpd.touched && form.controls.consentimentoLgpd.hasError('required')) {
            <div class="erro-lgpd">Aceite obrigatório (LGPD)</div>
          }
        </div>

        @if (erro()) {
          <div class="erro">{{ erro() }}</div>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions class="dialog-actions">
      <button mat-stroked-button mat-dialog-close id="btn-cancelar-novo-cliente">Cancelar</button>
      <button mat-raised-button color="primary" id="btn-salvar-novo-cliente"
              [disabled]="form.invalid || salvando()" (click)="salvar()">
        @if (salvando()) { <mat-spinner diameter="18"></mat-spinner> }
        Cadastrar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid { display: flex; flex-direction: column; gap: 4px; padding-top: 8px; }
    .full-width { width: 100%; }
    .erro { color: #c62828; font-size: 13px; padding: 4px 0; }
    .dialog-actions {
      display: flex;
      gap: 8px;
      padding: 0 16px 16px;
      width: 100%;
      box-sizing: border-box;
      justify-content: space-between;
    }
    .dialog-actions button { flex: 1; min-height: 48px; margin: 0 !important; }
    .lgpd-container { margin: 8px 0; display: flex; flex-direction: column; }
    .lgpd-checkbox { font-size: 13px; line-height: 1.3; }
    .lgpd-checkbox ::ng-deep .mdc-label { white-space: normal; color: #424242; }
    .erro-lgpd { color: #c62828; font-size: 12px; margin-top: 4px; padding-left: 32px; }
  `],
})
export class NovoClienteDialogComponent {
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    telefone: [''],
    cpf: [''],
    endereco: [''],
    consentimentoLgpd: [false, Validators.requiredTrue],
  });

  salvando = signal(false);
  erro = signal<string | null>(null);

  constructor(
    private clientesService: ClientesService,
    private dialogRef: MatDialogRef<NovoClienteDialogComponent>,
  ) {}

  salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const { nome, telefone, cpf, endereco, consentimentoLgpd } = this.form.getRawValue();
    this.clientesService.cadastrar({ nome, telefone: telefone || undefined, cpf: cpf || undefined, endereco: endereco || undefined, consentimentoLgpd }).subscribe({
      next: cliente => this.dialogRef.close(cliente),
      error: () => {
        this.salvando.set(false);
        this.erro.set('Erro ao cadastrar cliente. Tente novamente.');
      },
    });
  }
}
