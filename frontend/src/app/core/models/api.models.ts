/**
 * Mirror of the payloads the API returns.
 *
 * Kept in one file on purpose: these types are the contract with the backend, and having
 * them together makes it obvious when a rename on the server has not been followed here.
 */

export type UserRole = 'ADMIN' | 'BIBLIOTECARIO';
export type BookStatus = 'DISPONIBLE' | 'PRESTADO' | 'RESERVADO';
export type LoanStatus = 'ACTIVO' | 'POR_VENCER' | 'VENCIDO' | 'DEVUELTO';
export type ReservationStatus = 'PENDIENTE' | 'NOTIFICADO' | 'CANCELADO' | 'CUMPLIDO';
export type UserAccountStatus = 'ACTIVO' | 'ADVERTENCIA' | 'BLOQUEADO';

export interface AuthenticatedProfile {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  blockedUntil: string | null;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: AuthenticatedProfile;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  publicationYear: number | null;
  status: BookStatus;
  coverUrl: string | null;
  description: string | null;
  subjects: string[];
}

export interface BookPreview {
  isbn: string;
  title: string | null;
  subtitle: string | null;
  author: string | null;
  publicationYear: number | null;
  coverUrl: string | null;
  description: string | null;
  subjects: string[];
  alreadyInCatalogue: boolean;
}

export interface CreateBookRequest {
  isbn: string;
  title?: string | null;
  author?: string | null;
  publicationYear?: number | null;
  coverUrl?: string | null;
  description?: string | null;
  subjects?: string[];
}

export interface BookSummary {
  id: number;
  title: string;
  author: string;
  isbn: string;
  coverUrl: string | null;
}

export interface Loan {
  id: number;
  book: BookSummary;
  borrowerName: string;
  borrowerEmail: string;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  status: LoanStatus;
  daysUntilDue: number;
  daysLate: number;
}

export interface Reservation {
  id: number;
  book: BookSummary;
  requesterName: string;
  requesterEmail: string;
  requestedAt: string;
  status: ReservationStatus;
  notifiedAt: string | null;
  resolvedAt: string | null;
  queuePosition: number | null;
}

export interface BookRecommendation {
  book: Book;
  score: number;
  matchedSubjects: string[];
}

export interface AdminStats {
  catalogue: {
    total: number;
    available: number;
    borrowed: number;
    reserved: number;
  };
  loansByStatus: {
    active: number;
    dueSoon: number;
    overdue: number;
    returned: number;
  };
  blockedUsers: number;
  loansPerMonth: { month: string; label: string; total: number }[];
  topOverdueUsers: {
    id: number;
    name: string;
    email: string;
    lateReturns: number;
    status: UserAccountStatus;
    blockedUntil: string | null;
  }[];
}

export interface UserSummary {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  activeLoans: number;
  lateReturns: number;
  status: UserAccountStatus;
  blockedUntil: string | null;
  createdAt: string;
}

/** Error envelope produced by the backend's controller advice. */
export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
