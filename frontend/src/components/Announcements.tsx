"use client";

import Link from "next/link";
import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import {
  fetchNotificationsForCurrentUser,
  type NotificationDto,
} from "@/lib/api";

const BG_CLASSES = ["bg-sky-50", "bg-indigo-50", "bg-amber-50"];

const Announcements = () => {
  const { data: session } = useSession();
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    fetchNotificationsForCurrentUser(session)
      .then(({ data, error: err }) => {
        if (err) setError(err);
        else setItems(data ?? []);
      })
      .finally(() => setLoading(false));
  }, [session]);

  return (
    <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">Announcements</h2>
        <Link href="/list/announcements" className="text-xs text-indigo-600 hover:underline">
          View all
        </Link>
      </div>
      <div className="flex flex-col gap-3 mt-4">
        {loading ? (
          <div className="rounded-lg border border-gray-100 bg-gray-50/50 py-8 text-center">
            <p className="text-sm text-gray-500">Loading…</p>
          </div>
        ) : error ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50 py-6 text-center">
            <p className="text-sm text-amber-800">{error}</p>
            <p className="text-xs text-amber-600 mt-1">Notifications may not be available</p>
          </div>
        ) : items.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-200 bg-gray-50/50 py-8 text-center">
            <p className="text-sm text-gray-500">No announcements yet</p>
            <p className="text-xs text-gray-400 mt-1">They will appear here when published</p>
          </div>
        ) : (
          items.slice(0, 5).map((item, i) => (
            <div
              key={item.id}
              className={`rounded-lg p-3 border border-gray-100 ${BG_CLASSES[i % BG_CLASSES.length]}`}
            >
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-gray-800">{item.title}</h3>
                <span className="text-xs text-gray-500">
                  {item.createdAt
                    ? new Date(item.createdAt).toLocaleDateString(undefined, {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })
                    : "—"}
                </span>
              </div>
              <p className="text-sm text-gray-600 mt-1 line-clamp-2">{item.message}</p>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default Announcements;
