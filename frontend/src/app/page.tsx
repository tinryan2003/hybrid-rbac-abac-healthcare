import Link from "next/link";

const Homepage = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-6 p-8 bg-[#F7F8FA]">
      <h1 className="text-3xl font-bold text-gray-800">HMS</h1>
      <p className="text-gray-600 text-center max-w-md">
        Hospital Management System — Hospital management (Hybrid RBAC/ABAC).
      </p>
      <Link
        href="/sign-in"
        className="px-6 py-3 rounded-xl bg-indigo-600 text-white font-medium hover:bg-indigo-700"
      >
        Sign in
      </Link>
      <Link
        href="/admin"
        className="text-sm text-gray-500 hover:text-gray-700"
      >
        Go to Dashboard →
      </Link>
    </div>
  );
};

export default Homepage;