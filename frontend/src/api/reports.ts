import client from './client';

export type ReportType =
  | 'MONTHLY'
  | 'ANNUAL'
  | 'CATEGORY_WISE'
  | 'INCOME_EXPENSE'
  | 'SAVINGS'
  | 'BUDGET_PERFORMANCE';

export type ExportFormat = 'pdf' | 'csv' | 'json';

export interface ReportQueryParams {
  year?: number;
  month?: number;
  startDate?: string;
  endDate?: string;
  categoryId?: number;
  budgetId?: number;
  format?: ExportFormat;
}

export const reportsApi = {
  // Direct file download for PDF and CSV
  async downloadReport(
    reportType: ReportType,
    format: 'pdf' | 'csv',
    params: ReportQueryParams
  ): Promise<void> {
    const endpointMap: Record<ReportType, string> = {
      MONTHLY: '/reports/monthly',
      ANNUAL: '/reports/annual',
      CATEGORY_WISE: '/reports/category-wise',
      INCOME_EXPENSE: '/reports/income-expense',
      SAVINGS: '/reports/savings',
      BUDGET_PERFORMANCE: '/reports/budget-performance',
    };

    const endpoint = endpointMap[reportType];
    const query = new URLSearchParams();

    if (params.year) query.append('year', params.year.toString());
    if (params.month) query.append('month', params.month.toString());
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);
    if (params.categoryId) query.append('categoryId', params.categoryId.toString());
    if (params.budgetId) query.append('budgetId', params.budgetId.toString());
    query.append('format', format);

    const response = await client.get(`${endpoint}?${query.toString()}`, {
      responseType: 'blob',
    });

    // Extract filename from Content-Disposition if present
    let filename = `finwise-${reportType.toLowerCase().replace('_', '-')}.${format}`;
    const disposition = response.headers['content-disposition'];
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^";]+)"?/);
      if (match && match[1]) {
        filename = match[1];
      }
    }

    // Trigger browser download via object URL
    const blob = new Blob([response.data], {
      type: format === 'csv' ? 'text/csv;charset=utf-8;' : 'application/pdf',
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // Fetch JSON preview data
  async getPreviewData<T = any>(
    reportType: ReportType,
    params: ReportQueryParams
  ): Promise<T> {
    const endpointMap: Record<ReportType, string> = {
      MONTHLY: '/reports/monthly',
      ANNUAL: '/reports/annual',
      CATEGORY_WISE: '/reports/category-wise',
      INCOME_EXPENSE: '/reports/income-expense',
      SAVINGS: '/reports/savings',
      BUDGET_PERFORMANCE: '/reports/budget-performance',
    };

    const endpoint = endpointMap[reportType];
    const query = new URLSearchParams();

    if (params.year) query.append('year', params.year.toString());
    if (params.month) query.append('month', params.month.toString());
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);
    if (params.categoryId) query.append('categoryId', params.categoryId.toString());
    if (params.budgetId) query.append('budgetId', params.budgetId.toString());
    query.append('format', 'json');

    const response = await client.get(`${endpoint}?${query.toString()}`);
    return response.data.data;
  },
};
