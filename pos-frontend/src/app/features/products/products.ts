import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  Category,
  Product,
  ProductPayload,
  StockMovement,
  StockMovementType,
} from '../../core/models/catalogue.model';
import { Page, emptyPage } from '../../core/models/common.model';
import { CategoryService } from '../../core/services/category.service';
import { ProductService } from '../../core/services/product.service';
import { SettingsStore } from '../../core/services/settings.store';
import { ToastService } from '../../core/services/toast.service';
import { toFriendlyError } from '../../core/api/api-error';
import { MoneyPipe } from '../../shared/money.pipe';

type SortKey = 'name' | 'sku' | 'price' | 'stockQuantity' | 'updatedAt';

/**
 * Catalogue admin: search, create, edit, deactivate, and correct stock.
 *
 * <p>Stock is never edited through the product form. It moves only via an adjustment,
 * which writes a journal row, so a surprising count can always be traced back.</p>
 */
@Component({
  selector: 'app-products',
  imports: [ReactiveFormsModule, MoneyPipe],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly settings = inject(SettingsStore).settings;

  protected readonly page = signal<Page<Product>>(emptyPage<Product>());
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);

  protected readonly search = signal('');
  protected readonly categoryFilter = signal<number | null>(null);
  protected readonly activeFilter = signal<'all' | 'active' | 'inactive'>('active');
  protected readonly lowStockOnly = signal(false);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly sortKey = signal<SortKey>('name');
  protected readonly sortDesc = signal(false);

  /** Null means "create". */
  protected readonly editing = signal<Product | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly serverFieldErrors = signal<Record<string, string>>({});

  protected readonly stockTarget = signal<Product | null>(null);
  protected readonly movements = signal<StockMovement[]>([]);
  protected readonly movementsLoading = signal(false);

  protected readonly confirmingDelete = signal<Product | null>(null);

  protected readonly movementTypes: readonly { value: StockMovementType; label: string }[] = [
    { value: 'PURCHASE', label: 'Delivery / purchase' },
    { value: 'ADJUSTMENT', label: 'Count correction' },
    { value: 'RETURN', label: 'Customer return' },
    { value: 'SHRINKAGE', label: 'Damage or loss' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    sku: ['', [Validators.required, Validators.maxLength(40)]],
    barcode: [''],
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    cost: [0, [Validators.min(0)]],
    taxRate: [0, [Validators.min(0), Validators.max(100)]],
    stockQuantity: [0, [Validators.min(0)]],
    reorderLevel: [0, [Validators.min(0)]],
    unit: ['pc', [Validators.maxLength(16)]],
    categoryId: [null as number | null],
    active: [true],
    trackStock: [true],
  });

  protected readonly stockForm = this.fb.nonNullable.group({
    direction: ['increase' as 'increase' | 'decrease'],
    quantity: [1, [Validators.required, Validators.min(1)]],
    type: ['PURCHASE' as StockMovementType, Validators.required],
    reason: [''],
  });

  constructor() {
    this.loadCategories();
    this.load();
  }

  protected readonly isEditing = computed(() => this.editing() !== null);

  protected readonly rangeLabel = computed(() => {
    const current = this.page();
    if (current.totalElements === 0) {
      return 'No products';
    }
    const from = current.page * current.size + 1;
    const to = Math.min(from + current.content.length - 1, current.totalElements);
    return `${from}–${to} of ${current.totalElements}`;
  });

  // --- Listing -----------------------------------------------------------

  protected load(): void {
    this.loading.set(true);
    const activeFilter = this.activeFilter();

    this.productService
      .search({
        search: this.search().trim(),
        categoryId: this.categoryFilter(),
        active: activeFilter === 'all' ? null : activeFilter === 'active',
        lowStock: this.lowStockOnly() ? true : null,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: `${this.sortKey()},${this.sortDesc() ? 'desc' : 'asc'}`,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.toasts.fromError(error, 'Could not load products');
        },
      });
  }

  /** Any filter change resets to the first page; staying on page 5 of 2 shows nothing. */
  protected applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected onSearchInput(value: string): void {
    this.search.set(value);
    this.applyFilters();
  }

  protected onCategoryFilter(value: string): void {
    this.categoryFilter.set(value ? Number(value) : null);
    this.applyFilters();
  }

  protected onActiveFilter(value: string): void {
    this.activeFilter.set(value as 'all' | 'active' | 'inactive');
    this.applyFilters();
  }

  protected toggleLowStock(): void {
    this.lowStockOnly.update((current) => !current);
    this.applyFilters();
  }

  protected sortBy(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDesc.update((current) => !current);
    } else {
      this.sortKey.set(key);
      this.sortDesc.set(false);
    }
    this.load();
  }

  protected goToPage(index: number): void {
    const target = Math.max(0, Math.min(index, this.page().totalPages - 1));
    this.pageIndex.set(target);
    this.load();
  }

  private loadCategories(): void {
    this.categoryService.list().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error: unknown) => this.toasts.fromError(error, 'Could not load categories'),
    });
  }

  // --- Create / edit -----------------------------------------------------

  protected openCreate(): void {
    this.editing.set(null);
    this.serverFieldErrors.set({});
    this.form.reset({
      sku: '',
      barcode: '',
      name: '',
      description: '',
      price: 0,
      cost: 0,
      taxRate: this.settings().defaultTaxRate ?? 0,
      stockQuantity: 0,
      reorderLevel: 0,
      unit: 'pc',
      categoryId: null,
      active: true,
      trackStock: true,
    });
    this.form.controls.stockQuantity.enable();
    this.formOpen.set(true);
  }

  protected openEdit(product: Product): void {
    this.editing.set(product);
    this.serverFieldErrors.set({});
    this.form.reset({
      sku: product.sku,
      barcode: product.barcode ?? '',
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      cost: product.cost,
      taxRate: product.taxRate,
      stockQuantity: product.stockQuantity,
      reorderLevel: product.reorderLevel,
      unit: product.unit,
      categoryId: product.category?.id ?? null,
      active: product.active,
      trackStock: product.trackStock,
    });
    // Opening stock is only meaningful at creation time.
    this.form.controls.stockQuantity.disable();
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
    const payload: ProductPayload = {
      sku: raw.sku.trim(),
      barcode: raw.barcode.trim() || null,
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      price: Number(raw.price),
      cost: Number(raw.cost),
      taxRate: Number(raw.taxRate),
      reorderLevel: Number(raw.reorderLevel),
      unit: raw.unit.trim() || 'pc',
      categoryId: raw.categoryId ? Number(raw.categoryId) : null,
      active: raw.active,
      trackStock: raw.trackStock,
      stockQuantity: this.isEditing() ? null : Number(raw.stockQuantity),
    };

    this.saving.set(true);
    this.serverFieldErrors.set({});
    const current = this.editing();
    const request = current
      ? this.productService.update(current.id, payload)
      : this.productService.create(payload);

    request.subscribe({
      next: (product) => {
        this.saving.set(false);
        this.closeForm();
        this.toasts.success(current ? `${product.name} updated` : `${product.name} added`);
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const friendly = toFriendlyError(error);
        // 400s carry field errors; show them inline instead of in a toast.
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
    if (field.hasError('min')) {
      return 'Must be zero or more';
    }
    if (field.hasError('max')) {
      return 'Value is too large';
    }
    if (field.hasError('maxlength')) {
      return 'Too long';
    }
    return 'Invalid value';
  }

  // --- Stock -------------------------------------------------------------

  protected openStock(product: Product): void {
    this.stockTarget.set(product);
    this.stockForm.reset({ direction: 'increase', quantity: 1, type: 'PURCHASE', reason: '' });
    this.loadMovements(product.id);
  }

  protected closeStock(): void {
    this.stockTarget.set(null);
    this.movements.set([]);
  }

  private loadMovements(productId: number): void {
    this.movementsLoading.set(true);
    this.productService.movements(productId, 0, 10).subscribe({
      next: (page) => {
        this.movements.set(page.content);
        this.movementsLoading.set(false);
      },
      error: () => this.movementsLoading.set(false),
    });
  }

  protected submitStock(): void {
    const product = this.stockTarget();
    if (!product || this.stockForm.invalid) {
      this.stockForm.markAllAsTouched();
      return;
    }
    const raw = this.stockForm.getRawValue();
    const magnitude = Math.abs(Math.floor(Number(raw.quantity)));
    const quantityChange = raw.direction === 'decrease' ? -magnitude : magnitude;

    this.saving.set(true);
    this.productService
      .adjustStock(product.id, {
        quantityChange,
        type: raw.type,
        reason: raw.reason.trim() || null,
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.stockTarget.set(updated);
          this.toasts.success(
            `${updated.name} is now at ${updated.stockQuantity} ${updated.unit}`,
          );
          this.stockForm.patchValue({ quantity: 1, reason: '' });
          this.loadMovements(updated.id);
          this.load();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.toasts.fromError(error, 'Stock adjustment failed');
        },
      });
  }

  /** Suggested default: a count correction usually goes with ADJUSTMENT. */
  protected onDirectionChange(direction: string): void {
    this.stockForm.patchValue({
      direction: direction as 'increase' | 'decrease',
      type: direction === 'decrease' ? 'SHRINKAGE' : 'PURCHASE',
    });
  }

  // --- Delete ------------------------------------------------------------

  protected confirmDelete(product: Product): void {
    this.confirmingDelete.set(product);
  }

  protected cancelDelete(): void {
    this.confirmingDelete.set(null);
  }

  protected deleteProduct(): void {
    const product = this.confirmingDelete();
    if (!product) {
      return;
    }
    this.saving.set(true);
    this.productService.remove(product.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        this.toasts.success(`${product.name} removed from the catalogue`);
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.confirmingDelete.set(null);
        this.toasts.fromError(error, 'Could not remove the product');
      },
    });
  }

  protected stockBadgeClass(product: Product): string {
    if (!product.trackStock) {
      return 'badge--info';
    }
    if (product.stockQuantity <= 0) {
      return 'badge--danger';
    }
    return product.lowStock ? 'badge--warning' : 'badge--success';
  }

  protected movementLabel(type: StockMovementType): string {
    return this.movementTypes.find((entry) => entry.value === type)?.label ?? type;
  }
}
