import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toFriendlyError } from '../../core/api/api-error';
import { Category, CategoryPayload } from '../../core/models/catalogue.model';
import { CategoryService } from '../../core/services/category.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Category admin. Categories drive the filter chips on the terminal, so the colour is a
 * real feature here rather than decoration.
 */
@Component({
  selector: 'app-categories',
  imports: [ReactiveFormsModule],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
})
export class Categories {
  private readonly service = inject(CategoryService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly editing = signal<Category | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly serverFieldErrors = signal<Record<string, string>>({});
  protected readonly confirmingDelete = signal<Category | null>(null);

  protected readonly palette = [
    '#2563eb',
    '#16a34a',
    '#d97706',
    '#dc2626',
    '#7c3aed',
    '#0891b2',
    '#db2777',
    '#65a30d',
  ];

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: [''],
    color: ['#2563eb'],
    displayOrder: [0, [Validators.min(0)]],
  });

  constructor() {
    this.load();
  }

  protected readonly isEditing = computed(() => this.editing() !== null);

  protected load(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toasts.fromError(error, 'Could not load categories');
      },
    });
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.serverFieldErrors.set({});
    // Default the order to the end of the list so new categories do not jump the queue.
    const nextOrder = this.categories().length
      ? Math.max(...this.categories().map((category) => category.displayOrder)) + 1
      : 1;
    this.form.reset({ name: '', description: '', color: '#2563eb', displayOrder: nextOrder });
    this.formOpen.set(true);
  }

  protected openEdit(category: Category): void {
    this.editing.set(category);
    this.serverFieldErrors.set({});
    this.form.reset({
      name: category.name,
      description: category.description ?? '',
      color: category.color ?? '#2563eb',
      displayOrder: category.displayOrder,
    });
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected pickColor(color: string): void {
    this.form.patchValue({ color });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: CategoryPayload = {
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      color: raw.color || null,
      displayOrder: Number(raw.displayOrder),
    };

    this.saving.set(true);
    this.serverFieldErrors.set({});
    const current = this.editing();
    const request = current
      ? this.service.update(current.id, payload)
      : this.service.create(payload);

    request.subscribe({
      next: (category) => {
        this.saving.set(false);
        this.closeForm();
        this.toasts.success(current ? `${category.name} updated` : `${category.name} created`);
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
    if (field.hasError('maxlength')) {
      return 'Too long';
    }
    return 'Invalid value';
  }

  protected confirmDelete(category: Category): void {
    this.confirmingDelete.set(category);
  }

  protected cancelDelete(): void {
    this.confirmingDelete.set(null);
  }

  protected deleteCategory(): void {
    const category = this.confirmingDelete();
    if (!category) {
      return;
    }
    this.saving.set(true);
    this.service.remove(category.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        this.toasts.success(`${category.name} deleted`);
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        // A 422 here means the category still has products; the message says which.
        this.toasts.fromError(error, 'Could not delete the category');
      },
    });
  }
}
