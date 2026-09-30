import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toFriendlyError } from '../../core/api/api-error';
import { Customer, CustomerPayload } from '../../core/models/catalogue.model';
import { Page, emptyPage } from '../../core/models/common.model';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Customer records. Optional on a sale, so nothing here blocks selling.
 */
@Component({
  selector: 'app-customers',
  imports: [ReactiveFormsModule],
  templateUrl: './customers.html',
  styleUrl: './customers.css',
})
export class Customers {
  private readonly service = inject(CustomerService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly page = signal<Page<Customer>>(emptyPage<Customer>());
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);

  protected readonly editing = signal<Customer | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly serverFieldErrors = signal<Record<string, string>>({});
  protected readonly confirmingDelete = signal<Customer | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    phone: [''],
    email: ['', [Validators.email]],
    address: [''],
    notes: [''],
  });

  constructor() {
    this.load();
  }

  protected readonly isEditing = computed(() => this.editing() !== null);

  protected readonly rangeLabel = computed(() => {
    const current = this.page();
    if (current.totalElements === 0) {
      return 'No customers';
    }
    const from = current.page * current.size + 1;
    const to = Math.min(from + current.content.length - 1, current.totalElements);
    return `${from}–${to} of ${current.totalElements}`;
  });

  protected load(): void {
    this.loading.set(true);
    this.service.search(this.search().trim(), this.pageIndex(), 20).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toasts.fromError(error, 'Could not load customers');
      },
    });
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.pageIndex.set(0);
    this.load();
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(Math.max(0, Math.min(index, this.page().totalPages - 1)));
    this.load();
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.serverFieldErrors.set({});
    this.form.reset({ name: '', phone: '', email: '', address: '', notes: '' });
    this.formOpen.set(true);
  }

  protected openEdit(customer: Customer): void {
    this.editing.set(customer);
    this.serverFieldErrors.set({});
    this.form.reset({
      name: customer.name,
      phone: customer.phone ?? '',
      email: customer.email ?? '',
      address: customer.address ?? '',
      notes: customer.notes ?? '',
    });
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: CustomerPayload = {
      name: raw.name.trim(),
      phone: raw.phone.trim() || null,
      email: raw.email.trim() || null,
      address: raw.address.trim() || null,
      notes: raw.notes.trim() || null,
    };

    this.saving.set(true);
    this.serverFieldErrors.set({});
    const current = this.editing();
    const request = current
      ? this.service.update(current.id, payload)
      : this.service.create(payload);

    request.subscribe({
      next: (customer) => {
        this.saving.set(false);
        this.closeForm();
        this.toasts.success(current ? `${customer.name} updated` : `${customer.name} added`);
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const friendly = toFriendlyError(error);
        this.serverFieldErrors.set(friendly.fieldErrors);
        if (Object.keys(friendly.fieldErrors).length === 0) {
          this.toasts.error(friendly.message, friendly.details);
        }
      },
    });
  }

  protected fieldError(control: keyof typeof this.form.controls): string | null {
    const serverErrors = this.serverFieldErrors();
    if (Object.prototype.hasOwnProperty.call(serverErrors, control)) {
      return serverErrors[control] ?? null;
    }
    const field = this.form.controls[control];
    if (!field.touched || field.valid) {
      return null;
    }
    if (field.hasError('required')) {
      return 'This field is required';
    }
    if (field.hasError('email')) {
      return 'Enter a valid email address';
    }
    return 'Invalid value';
  }

  protected confirmDelete(customer: Customer): void {
    this.confirmingDelete.set(customer);
  }

  protected cancelDelete(): void {
    this.confirmingDelete.set(null);
  }

  protected deleteCustomer(): void {
    const customer = this.confirmingDelete();
    if (!customer) {
      return;
    }
    this.saving.set(true);
    this.service.remove(customer.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        this.toasts.success(`${customer.name} deleted`);
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        this.toasts.fromError(error, 'Could not delete the customer');
      },
    });
  }
}
