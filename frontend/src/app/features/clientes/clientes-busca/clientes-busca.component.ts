import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { ClientesService } from '../clientes.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ClienteResumo } from '../../../core/models/cliente.model';
import { CurrencyBrPipe } from '../../../shared/pipes/currency-br.pipe';
import { NovoClienteDialogComponent } from './novo-cliente-dialog.component';

@Component({
  selector: 'app-clientes-busca',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatToolbarModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatCardModule,
    MatProgressSpinnerModule, MatListModule, MatChipsModule,
    MatDialogModule, MatTooltipModule, CurrencyBrPipe,
  ],
  templateUrl: './clientes-busca.component.html',
  styleUrl: './clientes-busca.component.scss',
})
export class ClientesBuscaComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({ busca: [''] });

  clientes = signal<ClienteResumo[]>([]);
  carregando = signal(false);
  totalEncontrados = signal(0);
  buscaAtiva = signal('');

  constructor(
    private clientesService: ClientesService,
    public auth: AuthService,
    private router: Router,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    // debounceTime: evita chamada a cada tecla (conforme instrução do doc frontend)
    this.form.controls.busca.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(termo => {
        this.carregando.set(true);
        this.buscaAtiva.set(termo);
        return this.clientesService.buscar(termo);
      }),
      takeUntil(this.destroy$),
    ).subscribe({
      next: page => {
        this.clientes.set(page.content);
        this.totalEncontrados.set(page.totalElements);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });

    // Carrega lista inicial
    this.clientesService.buscar('').subscribe(page => {
      this.clientes.set(page.content);
      this.totalEncontrados.set(page.totalElements);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  abrirCliente(id: string) {
    this.router.navigate(['/clientes', id]);
  }

  abrirNovoCliente() {
    const ref = this.dialog.open(NovoClienteDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe(criado => {
      if (criado) this.router.navigate(['/clientes', criado.id]);
    });
  }

  irParaDashboard() {
    this.router.navigate(['/dashboard']);
  }

  logout() {
    this.auth.logout();
  }
}
