interface Transaction {
    fromId: string;
    toId: string;
    from: string;
    to: string;
    amount: number;
}
export declare const calculateSimplifiedDebts: (groupId: string) => Promise<Transaction[]>;
export {};
//# sourceMappingURL=settlementService.d.ts.map