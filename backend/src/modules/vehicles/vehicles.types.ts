/** Module-owned entity. Repositories map Prisma rows to this. */
export interface Vehicle {
  id: string;
  vinHash: string;
  modelKey: string;
  consentAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}
