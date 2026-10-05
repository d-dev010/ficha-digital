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

const PAGE_SIZE = 20;

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
  totalPaginas = signal(0);
  paginaAtual = signal(0);
  buscaAtiva = signal('');

  constructor(
    private clientesService: ClientesService,
    public auth: AuthService,
    private router: Router,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    // Ao digitar: reseta para a página 0 e recarrega
    this.form.controls.busca.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(termo => {
        this.carregando.set(true);
        this.buscaAtiva.set(termo);
        this.paginaAtual.set(0);
        return this.clientesService.buscar(termo, 0, PAGE_SIZE);
      }),
      takeUntil(this.destroy$),
    ).subscribe({
      next: page => {
        this.clientes.set(page.content);
        this.totalEncontrados.set(page.totalElements);
        this.totalPaginas.set(page.totalPages);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });

    // Carrega lista inicial
    this.carregarPagina(0);
  }

  /** Carrega uma página específica mantendo o termo de busca atual. */
  carregarPagina(pagina: number) {
    this.carregando.set(true);
    this.paginaAtual.set(pagina);
    const termo = this.form.controls.busca.value;
    this.clientesService.buscar(termo, pagina, PAGE_SIZE)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          this.clientes.set(page.content);
          this.totalEncontrados.set(page.totalElements);
          this.totalPaginas.set(page.totalPages);
          this.carregando.set(false);
          // Scroll suave ao topo da lista ao trocar página
          window.scrollTo({ top: 0, behavior: 'smooth' });
        },
        error: () => this.carregando.set(false),
      });
  }

  paginaAnterior() {
    if (this.paginaAtual() > 0) this.carregarPagina(this.paginaAtual() - 1);
  }

  proximaPagina() {
    if (this.paginaAtual() < this.totalPaginas() - 1) this.carregarPagina(this.paginaAtual() + 1);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  abrirCliente(id: string) {
    this.router.navigate(['/clientes', id]);
  }

  abrirNovoCliente() {
    const ref = this.dialog.open(NovoClienteDialogComponent, { 
      width: '92vw', 
      maxWidth: '440px', 
      disableClose: false 
    });
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
