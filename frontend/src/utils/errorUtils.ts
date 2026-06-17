export function getUserFriendlyError(error: any): string {
    const message =
        (typeof error === 'string' ? error : '') ||
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.message ||
        error?.error ||
        '';

    let cleaned = message.trim();
    let previous = '';

    while (cleaned && cleaned !== previous) {
        previous = cleaned;
        cleaned = cleaned
            .replace(/^[a-zA-Z0-9_.$]+(?:Exception|Error):\s*/i, '')
            .trim();
    }

    return cleaned;
}
