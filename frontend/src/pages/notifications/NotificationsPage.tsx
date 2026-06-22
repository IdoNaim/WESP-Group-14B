import { useState, useEffect } from 'react';
import { NotificationAPI, NotificationDTO } from '../../api/notificationsApi';

export default function NotificationsPage() {
    const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
    const [filter, setFilter] = useState<'All' | 'Unread'>('All');
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        fetchNotifications();
    }, []);

    const fetchNotifications = async () => {
        try {
            setIsLoading(true);
            const data = await NotificationAPI.getNotifications();
            setNotifications(data);
        } catch (error) {
            console.error('Failed to fetch notifications:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleMarkAsRead = async (id: string) => {
        const token = localStorage.getItem('token') || ''; 

        if (!token) return;

        setNotifications(prev => 
            prev.map(notif => notif.id === id ? { ...notif, read: true } : notif)
        );

        try {
            await NotificationAPI.markAsRead(id);
        } catch (error) {
            console.error('API Error: Failed to mark as read:', error);
            // שחזור במקרה של שגיאה מהשרת
            setNotifications(prev => 
                prev.map(notif => notif.id === id ? { ...notif, read: false } : notif)
            );
        }
    };

    const handleMarkAllAsRead = async () => {
        const unreadNotifications = notifications.filter(n => !n.read);
        
        // עדכון אופטימי לכל הרשימה
        setNotifications(prev => prev.map(notif => ({ ...notif, read: true })));
        
        try {
            await Promise.all(unreadNotifications.map(n => NotificationAPI.markAsRead(n.id)));
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    };

    const filteredNotifications = notifications.filter(notif => 
        filter === 'All' ? true : !notif.read
    );

    return (
        <div className="w-full min-h-screen bg-[#0f172a] text-gray-100 antialiased flex flex-col items-center">
            <main className="w-full pt-8 pb-12 flex justify-center">
                <div className="w-full px-8">
                    
                    {/* Header */}
                    <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
                        <div>
                            <h1 className="text-3xl font-bold text-white mb-4">Inbox</h1>
                            <div className="flex gap-2 border-b border-gray-700">
                                <button 
                                    onClick={() => setFilter('All')}
                                    className={`px-4 py-2 border-b-2 text-sm font-medium transition-colors ${filter === 'All' ? 'border-blue-500 text-blue-400' : 'border-transparent text-gray-400 hover:text-white'}`}
                                >
                                    All
                                </button>
                                <button 
                                    onClick={() => setFilter('Unread')}
                                    className={`px-4 py-2 border-b-2 text-sm font-medium transition-colors ${filter === 'Unread' ? 'border-blue-500 text-blue-400' : 'border-transparent text-gray-400 hover:text-white'}`}
                                >
                                    Unread
                                </button>
                            </div>
                        </div>
                        <button 
                            onClick={handleMarkAllAsRead}
                            className="shrink-0 py-2 px-4 bg-[#1e293b] border border-gray-600 shadow-sm rounded-lg text-sm font-medium text-gray-200 hover:bg-[#334155] transition-all flex items-center gap-2"
                        >
                            <span className="material-symbols-outlined text-[18px]">checklist</span>
                            Mark all as read
                        </button>
                    </div>

                    {isLoading && (
                        <div className="flex justify-center py-10 w-full">
                            <span className="text-gray-400">Loading notifications...</span>
                        </div>
                    )}

                    {!isLoading && filteredNotifications.length > 0 && (
                        <div className="flex flex-col gap-3 w-full">
                            {filteredNotifications.map(notification => (
                                <NotificationItem 
                                    key={notification.id} 
                                    notification={notification} 
                                    onMarkRead={() => handleMarkAsRead(notification.id)} 
                                />
                            ))}
                        </div>
                    )}

                    {!isLoading && filteredNotifications.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-12 mt-8 text-center w-full">
                            <div className="w-16 h-16 rounded-full bg-[#1e293b] flex items-center justify-center mb-4 text-gray-400">
                                <span className="material-symbols-outlined text-[32px]">task_alt</span>
                            </div>
                            <h3 className="text-xl font-bold text-white mb-2">You're all caught up!</h3>
                            <p className="text-gray-400 max-w-sm">
                                There are no {filter === 'Unread' ? 'unread' : 'new'} notifications at this time.
                            </p>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}

// ==========================================
// Sub-Component: Notification Item
// ==========================================
interface NotificationItemProps {
    notification: NotificationDTO;
    onMarkRead: () => void;
}

function NotificationItem({ notification, onMarkRead }: NotificationItemProps) {
    const isUnread = !notification.read;

    return (
        <div className={`group w-full bg-[#1e293b] border border-gray-700 rounded-lg p-4 flex items-start gap-4 transition-all hover:bg-[#334155] relative ${isUnread ? 'shadow-lg' : 'opacity-60 border-gray-800'}`}>
            <div className={`w-2 h-2 rounded-full mt-2 shrink-0 ${isUnread ? 'bg-blue-500' : 'bg-transparent'}`}></div>
            
            <div className="flex-1 min-w-0">
                <div className="flex justify-between items-start gap-4 mb-1">
                    <h3 className={`text-base font-semibold truncate ${isUnread ? 'text-white' : 'text-gray-400'}`}>
                        {notification.title || 'System Notification'}
                    </h3>
                    <span className={`text-xs shrink-0 ${isUnread ? 'text-gray-400' : 'text-gray-500'}`}>
                        {new Date(notification.createdAt).toLocaleDateString()}
                    </span>
                </div>
                
                <p className={`line-clamp-2 text-sm ${isUnread ? 'text-gray-300 font-medium' : 'text-gray-500'}`}>
                    {notification.message}
                </p>
            </div>

            {/* Action Button (Visible only on hover if unread) */}
            {isUnread && (
                <div className="opacity-0 group-hover:opacity-100 absolute right-4 top-1/2 -translate-y-1/2 transition-opacity focus-within:opacity-100">
                    <button 
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            onMarkRead();
                        }}
                        className="p-2 bg-[#0f172a] border border-gray-600 rounded shadow-sm text-gray-300 hover:text-blue-400 hover:border-blue-500 transition-colors"
                        title="Mark as read"
                    >
                        <span className="material-symbols-outlined text-[18px]">done</span>
                    </button>
                </div>
            )}
        </div>
    );
}