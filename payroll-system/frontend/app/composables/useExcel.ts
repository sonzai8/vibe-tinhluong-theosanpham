export const useExcel = () => {
    const { $api } = useNuxtApp();
    const config = useRuntimeConfig();

    const downloadTemplate = async (endpoint: string, filename: string) => {
        try {
            const data: any = await $api.get(endpoint, {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', filename);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            console.error('Download template error:', error);
            throw error;
        }
    };

    const importExcel = async (endpoint: string, file: File) => {
        const formData = new FormData();
        formData.append('file', file);
        try {
            return await $api.post(endpoint, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
        } catch (error) {
            console.error('Import error:', error);
            throw error;
        }
    };

    const exportExcel = async (endpoint: string, filename: string) => {
        try {
            const data: any = await $api.get(endpoint, {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', filename);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            console.error('Export error:', error);
            throw error;
        }
    };

    return {
        downloadTemplate,
        importExcel,
        exportExcel
    };
};
