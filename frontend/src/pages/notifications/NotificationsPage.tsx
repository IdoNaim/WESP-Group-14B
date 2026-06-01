import React, { useState, useEffect } from 'react';
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

        if (!token) {
            console.error('No token found!');
            return;
        }

        // תוקן: מעדכנים את השדה 'read' ל-true
        setNotifications(prev => 
            prev.map(notif => notif.id === id ? { ...notif, read: true } : notif)
        );

        try {
            await NotificationAPI.markAsRead(id);
            console.log(`Notification ${id} successfully marked as read.`);
        } catch (error) {
            console.error('API Error: Failed to mark as read:', error);
            
            // שחזור במקרה של שגיאה
            setNotifications(prev => 
                prev.map(notif => notif.id === id ? { ...notif, read: false } : notif)
            );
        }
    };

    // פונקציה חדשה: סימון כלא-נקרא
    const handleMarkAsUnread = async (id: string) => {
        const token = localStorage.getItem('token') || ''; 

        if (!token) {
            console.error('No token found!');
            return;
        }

        // עדכון אופטימי - הופכים חזרה ל-false
        setNotifications(prev => 
            prev.map(notif => notif.id === id ? { ...notif, read: false } : notif)
        );

        try {
            // תזדקק לפונקציה מתאימה ב-API שלך וב-Controller
            await NotificationAPI.markAsUnread(id);
            console.log(`Notification ${id} successfully marked as unread.`);
        } catch (error) {
            console.error('API Error: Failed to mark as unread:', error);
            
            // שחזור במקרה של שגיאה
            setNotifications(prev => 
                prev.map(notif => notif.id === id ? { ...notif, read: true } : notif)
            );
        }
    };

    const handleMarkAllAsRead = async () => {
        const unreadNotifications = notifications.filter(n => !n.read);
        
        // תוקן: מעדכנים את השדה 'read'
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
        <div className="bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100 min-h-screen antialiased overflow-x-hidden">
            
            {/* TopNavBar */}
            <nav className="flex justify-between items-center w-full px-6 h-16 fixed top-0 z-50 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 transition-colors">
                <div className="flex items-center gap-4">
                    <span className="text-2xl font-bold text-blue-600 dark:text-blue-400">Signal</span>
                    <div className="hidden md:flex relative items-center ml-8">
                        <span className="material-symbols-outlined absolute left-3 text-gray-500 text-[20px]">search</span>
                        <input 
                            className="pl-10 pr-4 py-2 bg-gray-100 dark:bg-gray-700 border-none rounded-full focus:ring-2 focus:ring-blue-500 transition-all text-sm text-gray-900 dark:text-white w-64 placeholder:text-gray-500" 
                            placeholder="Search..." 
                            type="text" 
                        />
                    </div>
                </div>
                <div className="hidden md:flex items-center gap-6">
                    <a className="text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors text-sm font-medium" href="#">Dashboard</a>
                    <a className="text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors text-sm font-medium" href="#">Settings</a>
                </div>
                <div className="flex items-center gap-2">
                    <button className="p-2 rounded-full text-blue-600 dark:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                        <span className="material-symbols-outlined" style={{fontVariationSettings: "'FILL' 1"}}>notifications</span>
                    </button>
                    <button className="p-2 rounded-full text-gray-500 hover:text-blue-600 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                        <span className="material-symbols-outlined">help</span>
                    </button>
                    <button className="p-2 rounded-full text-gray-500 hover:text-blue-600 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                        <span className="material-symbols-outlined">account_circle</span>
                    </button>
                </div>
            </nav>

            {/* SideNavBar */}
            <aside className="fixed left-0 top-16 h-[calc(100vh-64px)] w-64 flex-col border-r border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 hidden md:flex z-40">
                <div className="p-6 border-b border-gray-200 dark:border-gray-700">
                    <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Notifications</h2>
                    <p className="text-xs text-gray-500 mt-1">Manage your alerts</p>
                </div>
                <nav className="flex-1 px-2 py-4 flex flex-col gap-1 overflow-y-auto">
                    <a className="flex items-center gap-3 px-3 py-2 bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300 font-semibold rounded-lg transition-transform duration-150 active:scale-[0.98]" href="#">
                        <span className="material-symbols-outlined text-[20px]" style={{fontVariationSettings: "'FILL' 1"}}>inbox</span>
                        <span className="text-sm">Inbox</span>
                    </a>
                    <a className="flex items-center gap-3 px-3 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors" href="#">
                        <span className="material-symbols-outlined text-[20px]">alternate_email</span>
                        <span className="text-sm">Mentions</span>
                    </a>
                    <a className="flex items-center gap-3 px-3 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors" href="#">
                        <span className="material-symbols-outlined text-[20px]">report</span>
                        <span className="text-sm">System Alerts</span>
                    </a>
                </nav>
                <div className="p-4 border-t border-gray-200 dark:border-gray-700">
                    <button 
                        onClick={handleMarkAllAsRead}
                        className="w-full py-2 px-4 border border-gray-300 dark:border-gray-600 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors flex justify-center items-center gap-2"
                    >
                        <span className="material-symbols-outlined text-[18px]">done_all</span>
                        Mark all as read
                    </button>
                </div>
            </aside>

            {/* Main Content Canvas */}
            <main className="md:ml-64 pt-16 min-h-screen flex justify-center">
                <div className="w-full max-w-3xl px-6 py-8">
                    
                    {/* Page Header & Controls */}
                    <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">Inbox</h1>
                            <div className="flex gap-2 border-b border-gray-200 dark:border-gray-700">
                                <button 
                                    onClick={() => setFilter('All')}
                                    className={`px-4 py-2 border-b-2 text-sm font-medium transition-colors ${filter === 'All' ? 'border-blue-600 text-blue-600 dark:text-blue-400' : 'border-transparent text-gray-500 hover:text-gray-900 dark:hover:text-white'}`}
                                >
                                    All
                                </button>
                                <button 
                                    onClick={() => setFilter('Unread')}
                                    className={`px-4 py-2 border-b-2 text-sm font-medium transition-colors ${filter === 'Unread' ? 'border-blue-600 text-blue-600 dark:text-blue-400' : 'border-transparent text-gray-500 hover:text-gray-900 dark:hover:text-white'}`}
                                >
                                    Unread
                                </button>
                            </div>
                        </div>
                        <button 
                            onClick={handleMarkAllAsRead}
                            className="shrink-0 py-2 px-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm rounded-lg text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all flex items-center gap-2"
                        >
                            <span className="material-symbols-outlined text-[18px]">checklist</span>
                            Mark all as read
                        </button>
                    </div>

                    {/* Loading State */}
                    {isLoading && (
                        <div className="flex justify-center py-10">
                            <span className="text-gray-500">Loading notifications...</span>
                        </div>
                    )}

                    {/* Notification List */}
                    {!isLoading && filteredNotifications.length > 0 && (
                        <div className="flex flex-col gap-3">
                            {filteredNotifications.map(notification => (
                                <NotificationItem 
                                    key={notification.id} 
                                    notification={notification} 
                                    onMarkRead={() => handleMarkAsRead(notification.id)} 
                                    onMarkUnread={() => handleMarkAsUnread(notification.id)}
                                />
                            ))}
                        </div>
                    )}

                    {/* Empty State */}
                    {!isLoading && filteredNotifications.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-12 mt-8 text-center">
                            <div className="w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center mb-4 text-gray-400">
                                <span className="material-symbols-outlined text-[32px]">task_alt</span>
                            </div>
                            <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">You're all caught up!</h3>
                            <p className="text-gray-500 max-w-sm">
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
    onMarkUnread: () => void;
}

function NotificationItem({ notification, onMarkRead, onMarkUnread }: NotificationItemProps) {
    const isUnread = !notification.read;

    return (
        <div className={`group bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 flex items-start gap-4 transition-all hover:bg-gray-50 dark:hover:bg-gray-700/50 ${isUnread ? 'shadow-sm relative' : 'opacity-70'}`}>
            {/* Indicator Dot */}
            <div className={`w-2 h-2 rounded-full mt-2 shrink-0 ${isUnread ? 'bg-blue-600' : 'bg-transparent'}`}></div>
            
            <div className="flex-1 min-w-0">
                <div className="flex justify-between items-start gap-4 mb-1">
                    <h3 className={`text-base font-semibold truncate ${isUnread ? 'text-gray-900 dark:text-white' : 'text-gray-600 dark:text-gray-400'}`}>
                        {notification.title || 'System Notification'}
                    </h3>
                    <span className={`text-xs shrink-0 ${isUnread ? 'text-gray-500 dark:text-gray-400' : 'text-gray-400 dark:text-gray-500'}`}>
                        {new Date(notification.createdAt).toLocaleDateString()}
                    </span>
                </div>
                
                <p className={`line-clamp-2 text-sm ${isUnread ? 'text-gray-700 dark:text-gray-300 font-medium' : 'text-gray-500 dark:text-gray-400'}`}>
                    {notification.message}
                </p>
            </div>

            {/* Action Buttons (Visible only on hover) */}
            <div className="opacity-0 group-hover:opacity-100 absolute right-4 top-1/2 -translate-y-1/2 transition-opacity focus-within:opacity-100">
                {isUnread ? (
                    <button 
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            onMarkRead();
                        }}
                        className="p-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded shadow-sm text-gray-500 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        title="Mark as read"
                    >
                        <span className="material-symbols-outlined text-[18px]">done</span>
                    </button>
                ) : (
                    <button 
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            onMarkUnread();
                        }}
                        className="p-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded shadow-sm text-gray-500 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        title="Mark as unread"
                    >
                        <span className="material-symbols-outlined text-[18px]">mark_email_unread</span>
                    </button>
                )}
            </div>
        </div>
    );
}