"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import Table from "@/components/Table";
import {
  fetchInvoices,
  fetchPayments,
  fetchPatients,
  createInvoice,
  createPayment,
  updateInvoiceStatus,
  type InvoiceDto,
  type PaymentDto,
  type PatientDto,
  type CreateInvoiceRequest,
  type CreatePaymentRequest,
} from "@/lib/api";

const invoiceStatusLabel: Record<string, string> = {
  PENDING: "Pending",
  PAID: "Paid",
  PARTIALLY_PAID: "Partially paid",
  CANCELLED: "Cancelled",
  REFUNDED: "Refunded",
};

const paymentStatusLabel: Record<string, string> = {
  PENDING: "Processing",
  COMPLETED: "Completed",
  FAILED: "Failed",
  REFUNDED: "Refunded",
};

const paymentMethodLabel: Record<string, string> = {
  CASH: "Cash",
  CREDIT_CARD: "Credit card",
  DEBIT_CARD: "Debit card",
  BANK_TRANSFER: "Bank transfer",
  INSURANCE: "Insurance",
  OTHER: "Other",
};

const ReceptionistPage = () => {
  const { data: session } = useSession();
  const [invoices, setInvoices] = useState<InvoiceDto[]>([]);
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"invoices" | "payments">("invoices");
  const [invoiceStatusFilter, setInvoiceStatusFilter] = useState<string>("ALL");
  const [paymentStatusFilter, setPaymentStatusFilter] = useState<string>("ALL");
  const [refreshKey, setRefreshKey] = useState(0);

  // Create invoice form state
  const [showCreateInvoice, setShowCreateInvoice] = useState(false);
  const [invoiceForm, setInvoiceForm] = useState({
    patientId: "",
    description: "",
    quantity: "1",
    unitPrice: "",
    notes: "",
    taxAmount: "",
  });
  const [creatingInvoice, setCreatingInvoice] = useState(false);
  const [invoiceFormError, setInvoiceFormError] = useState<string | null>(null);

  // Record payment form state
  const [showRecordPayment, setShowRecordPayment] = useState(false);
  const [paymentForm, setPaymentForm] = useState({
    invoiceId: "",
    amount: "",
    paymentMethod: "CASH",
    notes: "",
  });
  const [recordingPayment, setRecordingPayment] = useState(false);
  const [paymentFormError, setPaymentFormError] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      setError(null);
      try {
        const [invoicesRes, paymentsRes, patientsRes] = await Promise.all([
          fetchInvoices(session, {
            status: invoiceStatusFilter === "ALL" ? undefined : invoiceStatusFilter,
          }),
          fetchPayments(session, {
            status: paymentStatusFilter === "ALL" ? undefined : paymentStatusFilter,
          }),
          fetchPatients(session),
        ]);
        if (invoicesRes.error) setError(invoicesRes.error);
        else if (invoicesRes.data) {
          const sorted = [...invoicesRes.data].sort((a, b) => {
            const dateA = a.invoiceDate ? new Date(a.invoiceDate).getTime() : 0;
            const dateB = b.invoiceDate ? new Date(b.invoiceDate).getTime() : 0;
            return dateB - dateA;
          });
          setInvoices(sorted);
        }
        if (paymentsRes.data) {
          const sorted = [...paymentsRes.data].sort((a, b) => {
            const dateA = a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
            const dateB = b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
            return dateB - dateA;
          });
          setPayments(sorted);
        }
        if (patientsRes.data) setPatients(patientsRes.data);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session, invoiceStatusFilter, paymentStatusFilter, refreshKey]);

  const pendingInvoices = invoices.filter((i) => i.status === "PENDING").length;
  const totalPendingAmount = invoices
    .filter((i) => i.status === "PENDING")
    .reduce((sum, i) => sum + (i.totalAmount || 0), 0);
  const todayPayments = payments.filter((p) => {
    if (!p.paymentDate) return false;
    return new Date(p.paymentDate).toISOString().split("T")[0] === new Date().toISOString().split("T")[0];
  }).length;
  const totalCollectedToday = payments
    .filter((p) => {
      if (!p.paymentDate || p.status !== "COMPLETED") return false;
      return new Date(p.paymentDate).toISOString().split("T")[0] === new Date().toISOString().split("T")[0];
    })
    .reduce((sum, p) => sum + (p.amount || 0), 0);

  const handleCreateInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setInvoiceFormError(null);
    if (!invoiceForm.patientId || !invoiceForm.description || !invoiceForm.unitPrice) {
      setInvoiceFormError("Patient, description, and unit price are required.");
      return;
    }
    setCreatingInvoice(true);
    const payload: CreateInvoiceRequest = {
      patientId: parseInt(invoiceForm.patientId),
      notes: invoiceForm.notes || undefined,
      taxAmount: invoiceForm.taxAmount ? parseFloat(invoiceForm.taxAmount) : undefined,
      items: [
        {
          description: invoiceForm.description,
          quantity: parseInt(invoiceForm.quantity) || 1,
          unitPrice: parseFloat(invoiceForm.unitPrice),
          serviceType: "CONSULTATION",
        },
      ],
    };
    const { error: err } = await createInvoice(session, payload);
    setCreatingInvoice(false);
    if (err) {
      setInvoiceFormError(err);
    } else {
      setShowCreateInvoice(false);
      setInvoiceForm({ patientId: "", description: "", quantity: "1", unitPrice: "", notes: "", taxAmount: "" });
      setRefreshKey((k) => k + 1);
    }
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setPaymentFormError(null);
    if (!paymentForm.invoiceId || !paymentForm.amount) {
      setPaymentFormError("Invoice ID and amount are required.");
      return;
    }
    setRecordingPayment(true);
    const payload: CreatePaymentRequest = {
      invoiceId: parseInt(paymentForm.invoiceId),
      paymentMethod: paymentForm.paymentMethod,
      amount: parseFloat(paymentForm.amount),
      notes: paymentForm.notes || undefined,
    };
    const { error: err } = await createPayment(session, payload);
    setRecordingPayment(false);
    if (err) {
      setPaymentFormError(err);
    } else {
      setShowRecordPayment(false);
      setPaymentForm({ invoiceId: "", amount: "", paymentMethod: "CASH", notes: "" });
      setRefreshKey((k) => k + 1);
    }
  };

  const handleMarkPaid = async (invoiceId: number) => {
    if (!session) return;
    if (!confirm("Mark this invoice as PAID?")) return;
    const { error: err } = await updateInvoiceStatus(session, invoiceId, "PAID");
    if (err) setError(err);
    else setRefreshKey((k) => k + 1);
  };

  const invoiceColumns = [
    { header: "Invoice #", accessor: "invoiceNumber" },
    { header: "Patient", accessor: "patientName" },
    { header: "Date", accessor: "invoiceDate" },
    { header: "Total", accessor: "totalAmount" },
    { header: "Status", accessor: "status" },
    { header: "Actions", accessor: "actions" },
  ];

  const paymentColumns = [
    { header: "Invoice", accessor: "invoiceId" },
    { header: "Amount", accessor: "amount" },
    { header: "Method", accessor: "paymentMethod" },
    { header: "Date", accessor: "paymentDate" },
    { header: "Status", accessor: "status" },
  ];

  const renderInvoiceRow = (item: InvoiceDto) => (
    <tr key={item.invoiceId} className="border-b border-gray-200 hover:bg-gray-50 text-sm">
      <td className="p-2 font-medium">{item.invoiceNumber || `#${item.invoiceId}`}</td>
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.invoiceDate ? new Date(item.invoiceDate).toLocaleDateString("en-US") : "—"}</td>
      <td className="p-2 font-semibold">
        {item.totalAmount != null
          ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.totalAmount)
          : "—"}
      </td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "PAID" ? "bg-green-100 text-green-800" :
          item.status === "PARTIALLY_PAID" ? "bg-blue-100 text-blue-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          item.status === "CANCELLED" ? "bg-red-100 text-red-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status ? invoiceStatusLabel[item.status] ?? item.status : "Pending"}
        </span>
      </td>
      <td className="p-2">
        {item.status === "PENDING" && (
          <button
            type="button"
            onClick={() => handleMarkPaid(item.invoiceId!)}
            className="px-2 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
          >
            Mark paid
          </button>
        )}
      </td>
    </tr>
  );

  const renderPaymentRow = (item: PaymentDto) => (
    <tr key={item.paymentId} className="border-b border-gray-200 hover:bg-gray-50 text-sm">
      <td className="p-2">#{item.invoiceId}</td>
      <td className="p-2 font-semibold">
        {item.amount != null
          ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.amount)
          : "—"}
      </td>
      <td className="p-2">{item.paymentMethod ? paymentMethodLabel[item.paymentMethod] ?? item.paymentMethod : "—"}</td>
      <td className="p-2">{item.paymentDate ? new Date(item.paymentDate).toLocaleDateString("en-US") : "—"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "COMPLETED" ? "bg-green-100 text-green-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          item.status === "FAILED" ? "bg-red-100 text-red-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status ? paymentStatusLabel[item.status] ?? item.status : "Processing"}
        </span>
      </td>
    </tr>
  );

  return (
    <div className="flex-1 p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <h1 className="text-xl font-semibold text-gray-800">Receptionist — Billing</h1>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => { setShowCreateInvoice(true); setShowRecordPayment(false); }}
              className="px-4 py-2 bg-lamaSky text-white rounded-md text-sm hover:bg-sky-600"
            >
              + Create invoice
            </button>
            <button
              type="button"
              onClick={() => { setShowRecordPayment(true); setShowCreateInvoice(false); }}
              className="px-4 py-2 bg-green-600 text-white rounded-md text-sm hover:bg-green-700"
            >
              + Record payment
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Pending invoices</h3>
            <p className="text-2xl font-bold text-yellow-600">{pendingInvoices}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Pending amount</h3>
            <p className="text-sm font-bold text-lamaSky">
              {new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(totalPendingAmount)}
            </p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Payments today</h3>
            <p className="text-2xl font-bold text-blue-600">{todayPayments}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Collected today</h3>
            <p className="text-sm font-bold text-green-600">
              {new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(totalCollectedToday)}
            </p>
          </div>
        </div>

        {/* Create Invoice Form */}
        {showCreateInvoice && (
          <div className="bg-white p-4 rounded-md border border-lamaSky">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-semibold">Create invoice</h2>
              <button type="button" onClick={() => setShowCreateInvoice(false)} className="text-gray-400 hover:text-gray-600 text-lg">✕</button>
            </div>
            <form onSubmit={handleCreateInvoice} className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label htmlFor="inv-patient" className="block text-xs text-gray-600 mb-1">Patient *</label>
                <select
                  id="inv-patient"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  value={invoiceForm.patientId}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, patientId: e.target.value })}
                >
                  <option value="">— Select patient —</option>
                  {patients.map((p) => (
                    <option key={p.patientId} value={p.patientId}>
                      {p.firstname} {p.lastname} (#{p.patientId})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="inv-description" className="block text-xs text-gray-600 mb-1">Service description *</label>
                <input
                  id="inv-description"
                  type="text"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  placeholder="e.g. Consultation, Lab test..."
                  value={invoiceForm.description}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, description: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="inv-qty" className="block text-xs text-gray-600 mb-1">Quantity</label>
                <input
                  id="inv-qty"
                  type="number"
                  min="1"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  value={invoiceForm.quantity}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, quantity: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="inv-price" className="block text-xs text-gray-600 mb-1">Unit price (VND) *</label>
                <input
                  id="inv-price"
                  type="number"
                  min="0"
                  step="1000"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  placeholder="150000"
                  value={invoiceForm.unitPrice}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, unitPrice: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="inv-tax" className="block text-xs text-gray-600 mb-1">Tax amount (VND)</label>
                <input
                  id="inv-tax"
                  type="number"
                  min="0"
                  step="1000"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  placeholder="0"
                  value={invoiceForm.taxAmount}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, taxAmount: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="inv-notes" className="block text-xs text-gray-600 mb-1">Notes</label>
                <input
                  id="inv-notes"
                  type="text"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  value={invoiceForm.notes}
                  onChange={(e) => setInvoiceForm({ ...invoiceForm, notes: e.target.value })}
                />
              </div>
              <div className="md:col-span-2">
                {invoiceFormError && <p className="text-red-600 text-xs mb-2">{invoiceFormError}</p>}
                <button
                  type="submit"
                  disabled={creatingInvoice}
                  className="px-4 py-2 bg-lamaSky text-white rounded text-sm hover:bg-sky-600 disabled:opacity-50"
                >
                  {creatingInvoice ? "Creating..." : "Create invoice"}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Record Payment Form */}
        {showRecordPayment && (
          <div className="bg-white p-4 rounded-md border border-green-300">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-semibold">Record payment</h2>
              <button type="button" onClick={() => setShowRecordPayment(false)} className="text-gray-400 hover:text-gray-600 text-lg">✕</button>
            </div>
            <form onSubmit={handleRecordPayment} className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label htmlFor="pay-invoice" className="block text-xs text-gray-600 mb-1">Invoice ID *</label>
                <input
                  id="pay-invoice"
                  type="number"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  placeholder="Invoice ID"
                  value={paymentForm.invoiceId}
                  onChange={(e) => setPaymentForm({ ...paymentForm, invoiceId: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="pay-amount" className="block text-xs text-gray-600 mb-1">Amount (VND) *</label>
                <input
                  id="pay-amount"
                  type="number"
                  min="0"
                  step="1000"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  placeholder="150000"
                  value={paymentForm.amount}
                  onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })}
                />
              </div>
              <div>
                <label htmlFor="pay-method" className="block text-xs text-gray-600 mb-1">Payment method</label>
                <select
                  id="pay-method"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  value={paymentForm.paymentMethod}
                  onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}
                >
                  <option value="CASH">Cash</option>
                  <option value="CREDIT_CARD">Credit card</option>
                  <option value="DEBIT_CARD">Debit card</option>
                  <option value="BANK_TRANSFER">Bank transfer</option>
                  <option value="INSURANCE">Insurance</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div>
                <label htmlFor="pay-notes" className="block text-xs text-gray-600 mb-1">Notes</label>
                <input
                  id="pay-notes"
                  type="text"
                  className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                  value={paymentForm.notes}
                  onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })}
                />
              </div>
              <div className="md:col-span-2">
                {paymentFormError && <p className="text-red-600 text-xs mb-2">{paymentFormError}</p>}
                <button
                  type="submit"
                  disabled={recordingPayment}
                  className="px-4 py-2 bg-green-600 text-white rounded text-sm hover:bg-green-700 disabled:opacity-50"
                >
                  {recordingPayment ? "Recording..." : "Record payment"}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Tabs + filters */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-4 border-b pb-2">
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => setActiveTab("invoices")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "invoices" ? "border-b-2 border-lamaSky text-lamaSky" : "text-gray-500"
                }`}
              >
                Invoices ({invoices.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab("payments")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "payments" ? "border-b-2 border-lamaSky text-lamaSky" : "text-gray-500"
                }`}
              >
                Payments ({payments.length})
              </button>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-500 uppercase tracking-wide">Status</span>
              <select
                className="border border-gray-200 rounded-md px-2 py-1 text-sm bg-gray-50 focus:outline-none focus:ring-1 focus:ring-lamaSky"
                aria-label={activeTab === "invoices" ? "Filter invoices by status" : "Filter payments by status"}
                value={activeTab === "invoices" ? invoiceStatusFilter : paymentStatusFilter}
                onChange={(e) =>
                  activeTab === "invoices"
                    ? setInvoiceStatusFilter(e.target.value)
                    : setPaymentStatusFilter(e.target.value)
                }
              >
                <option value="ALL">All</option>
                <option value="PENDING">Pending</option>
                {activeTab === "invoices" ? (
                  <>
                    <option value="PAID">Paid</option>
                    <option value="PARTIALLY_PAID">Partially paid</option>
                    <option value="CANCELLED">Cancelled</option>
                    <option value="REFUNDED">Refunded</option>
                  </>
                ) : (
                  <>
                    <option value="COMPLETED">Completed</option>
                    <option value="FAILED">Failed</option>
                    <option value="REFUNDED">Refunded</option>
                  </>
                )}
              </select>
            </div>
          </div>

          {error && (
            <div className="py-4 px-3 rounded-lg bg-red-50 text-red-700 text-sm mb-3">{error}</div>
          )}

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "invoices" && (
                invoices.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    {invoiceStatusFilter === "ALL" ? "No invoices found" : `No ${invoiceStatusFilter.toLowerCase()} invoices`}
                  </div>
                ) : (
                  <Table columns={invoiceColumns} data={invoices} renderRow={renderInvoiceRow} />
                )
              )}
              {activeTab === "payments" && (
                payments.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    {paymentStatusFilter === "ALL" ? "No payments found" : `No ${paymentStatusFilter.toLowerCase()} payments`}
                  </div>
                ) : (
                  <Table columns={paymentColumns} data={payments} renderRow={renderPaymentRow} />
                )
              )}
            </>
          )}
        </div>
      </div>

      <div className="w-full xl:w-1/3 flex flex-col gap-8">
        <Announcements />
      </div>
    </div>
  );
};

export default ReceptionistPage;
